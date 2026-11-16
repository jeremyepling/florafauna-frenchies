package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class FrontpackFlightPlaytest {
    static void steps(Playtest t, UUID dogId) {
        t.server("Craft the stowed red pack with an elytra and wear it", p -> {
            var menu = new CraftingMenu(1, p.getInventory(), ContainerLevelAccess.create(p.level(), p.blockPosition()));
            var grid = menu.getInputGridSlots();
            grid.get(0).set(p.getInventory().removeItemNoUpdate(3));
            grid.get(1).set(new ItemStack(Items.ELYTRA));
            var winged = menu.getResultSlot().safeTake(1, 1, p);
            Playtest.require(winged.is(FrenchieRegistry.WINGED_FRONT_PACK.get()) && grid.stream().noneMatch(s -> s.hasItem()),
                    "The recipe did not consume the red pack and elytra");
            Playtest.require(p.getInventory().getItem(10).isEmpty(), "Expected space for the previous chestplate");
            p.getInventory().setItem(10, p.getItemBySlot(EquipmentSlot.CHEST).copyAndClear());
            p.setItemSlot(EquipmentSlot.CHEST, winged);
        });
        t.awaitClient("See the dyed winged pack in the chest slot", mc -> {
            var chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            return chest.is(FrenchieRegistry.WINGED_FRONT_PACK.get())
                    && DyedItemColor.getOrDefault(chest, 0) == DyeColor.RED.getTextureDiffuseColor();
        });
        FrontpackPlaytest.pickUpDog(t);
        t.awaitClient("Pick up the Frenchie using the winged pack", mc ->
                mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        FrontpackPlaytest.framePlayer(t);
        FrontpackCloseups.wingedPack(t);
        t.awaitClient("See the brindle head, dyed pouch and both elytra wings", mc ->
                visible(mc.gameDirectory.toPath().resolve("screenshots")));
        t.server("Move the carrier above the pen for a survival glide", p ->
                p.teleportTo(p.level(), p.getX(), 285, p.getZ(), Set.of(), 180, 0, true));
        t.awaitClient("Wait until the carrier is falling", mc -> mc.player.getY() > 250 && !mc.player.onGround()
                && mc.player.getDeltaMovement().y < -0.05);
        t.client("Press jump in midair to open the wings", mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setXRot(0);
            mc.options.keyJump.setDown(true);
        });
        t.awaitClient("See the winged carrier gliding", mc -> mc.player.isFallFlying() && mc.player.getFallFlyingTicks() >= 10);
        t.client("Release jump and keep flying", mc -> mc.options.keyJump.setDown(false));
        t.screenshot("frontpack-winged-glide-back");
        t.client("View the flying Frenchie from the front", mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.awaitClient("Wait for the front flight camera", mc -> mc.player.isFallFlying()
                && mc.gameRenderer.getMainCamera().position().z < mc.player.getZ());
        t.screenshot("frontpack-winged-glide-front");
        int[] cameraStarted = new int[1];
        t.client("Look up at the wings and the carried Frenchie from the front quarter", mc -> {
            var camera = new Marker(EntityType.MARKER, mc.level);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(camera);
            followGlider(mc);
            cameraStarted[0] = mc.player.tickCount;
        });
        t.awaitClient("Follow the gliding carrier while the camera settles", mc -> {
            followGlider(mc);
            return mc.player.isFallFlying() && mc.player.tickCount >= cameraStarted[0] + 12;
        });
        t.screenshot("frontpack-winged-glide-oblique", FrontpackFlightPlaytest::followGlider);
        FrontpackFlightPosePlaytest.capture(t);
        t.client("Return the flight camera to the player", mc -> mc.setCameraEntity(mc.player));
        t.awaitClient("See the red pouch and open wings together in flight", mc ->
                flightVisible(mc.gameDirectory.toPath().resolve("screenshots/frontpack-winged-glide-oblique.png")));
        t.awaitServer("Glide in survival while carrying the original Frenchie in the chest pack", p ->
                p.isFallFlying() && !p.getAbilities().flying && p.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.WINGED_FRONT_PACK.get())
                        && p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).frenchieNBT().read("UUID", UUIDUtil.CODEC).filter(dogId::equals).isPresent()
                        && p.level().getEntity(dogId) == null);
        t.server("Bring the gliding carrier down onto the starting pen", p ->
                p.teleportTo(p.level(), 0.5, 201.05, 2.5, Set.of(), 180, 0, true));
        t.awaitClient("Land with the same Frenchie still in the winged pack", mc -> mc.player.onGround() && !mc.player.isFallFlying()
                && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        FrontpackPlaytest.framePlayer(t);
        FrontpackCloseups.landed(t);
    }

    private static void followGlider(Minecraft mc) {
        var target = mc.player.position().add(0, 0.35, -0.25);
        var position = target.add(1.65, -1.8, -2.1);
        var aim = target.subtract(position);
        var camera = mc.getCameraEntity();
        camera.snapTo(position.add(0, -camera.getEyeHeight(), 0),
                (float) Math.toDegrees(Math.atan2(-aim.x, aim.z)), (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance())));
    }

    private static boolean flightVisible(Path frame) {
        try {
            var image = ImageIO.read(frame.toFile());
            if (image == null) return false;
            int h = image.getHeight(), center = image.getWidth() / 2, fabric = 0, wing = 0;
            for (int y = h / 8; y < h * 7 / 8; y++) {
                for (int x = center - h / 2; x < center + h / 2; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                    if (r > 50 && r > g * 1.9 && Math.abs(g - b) < 12) fabric++;
                    if (b > g && g > r + 4 && b < 160) wing++;
                }
            }
            Playtest.require(fabric > h * h / 6000 && wing > h * h / 2000,
                    "Expected the red pouch and blue-gray wings in the flight frame: red=" + fabric + ", wings=" + wing);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean visible(Path shots) {
        try {
            var front = ImageIO.read(shots.resolve("frontpack-winged-front.png").toFile());
            var back = ImageIO.read(shots.resolve("frontpack-winged-back.png").toFile());
            var plain = ImageIO.read(shots.resolve("frontpack-red-back.png").toFile());
            if (front == null || back == null || plain == null) return false;
            int h = front.getHeight(), center = front.getWidth() / 2, fur = 0, fabric = 0;
            for (int dx = -h / 20; dx < h / 20; dx++) {
                for (int y = h * 45 / 100; y < h * 55 / 100; y++) {
                    int c = front.getRGB(center + dx, y);
                    int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                    if (r < 125 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 12) fur++;
                }
                for (int y = h * 66 / 100; y < h * 74 / 100; y++) {
                    int c = front.getRGB(center + dx, y);
                    if ((c >> 16 & 255) - (c >> 8 & 255) > 25) fabric++;
                }
            }
            Playtest.require(fur > h * h / 350 && fabric > h * h / 350, "Expected a visible brindle face above the red winged pouch");
            for (int side : new int[]{-1, 1}) {
                int changed = 0;
                for (int dx = h / 50; dx < h / 10; dx++) {
                    for (int y = h * 44 / 100; y < h * 70 / 100; y++) {
                        int a = back.getRGB(center + side * dx, y), b = plain.getRGB(center + side * dx, y);
                        if (Math.abs((a & 255) - (b & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) > 35) changed++;
                    }
                }
                Playtest.require(changed > h * h / 300, "Missing folded wing on side " + side + ": " + changed + " changed pixels");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
