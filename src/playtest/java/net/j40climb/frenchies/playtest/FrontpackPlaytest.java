package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.test.FrenchiePen;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class FrontpackPlaytest {
    private static final UUID DOG_ID = UUID.fromString("bd500dac-4311-4a77-a145-20824d17806e");
    private static final BlockPos PEN = new BlockPos(0, 201, 0);
    private static final AABB TEST_AREA = new AABB(PEN).inflate(8);
    private static final FrenchieVariant COAT = FrenchieVariant.BRINDLE;

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Place the Frenchie plot and a tamed brindle in reach", p -> {
                var level = p.level().getServer().overworld();
                FrenchiePen.place(level, new BlockPos(-3, 200, -6));
                p.teleportTo(level, 0.5, 201, 2.5, Set.of(), 180, 0, true);
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                var dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                Playtest.require(dog != null, "Could not create Frenchie");
                var tag = new CompoundTag();
                tag.putInt("Variant", COAT.getId());
                dog.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                dog.setUUID(DOG_ID);
                dog.setNoAi(true);
                dog.tame(p);
                dog.snapTo(0.5, 201, 0.5, 0, 0);
                Playtest.require(level.addFreshEntity(dog), "Could not add Frenchie to plot");
            });
            t.client("Crouch in first person", mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.keyShift.setDown(true);
            });
            t.awaitClient("Aim the empty hand at the player's brindle Frenchie", mc -> {
                if (!(mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog)) return false;
                mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
                return mc.player.isShiftKeyDown() && dog.isOwnedBy(mc.player) && dog.getVariant() == COAT
                        && mc.player.getMainHandItem().isEmpty()
                        && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
            });
            t.client("Right-click the dog to pick it up", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        }
        t.awaitServer("Check the original dog is inside the pack and absent from the pen", p -> {
            var data = p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
            return data.hasCarriedFrenchie() && data.frenchieNBT().read("UUID", UUIDUtil.CODEC).filter(DOG_ID::equals).isPresent()
                    && data.frenchieNBT().getIntOr("Variant", -1) == COAT.getId() && dogCount(p) == 0;
        });
        t.awaitClient("Check the tracked pack uses the brindle variant", mc -> {
            return mc.level.getEntity(DOG_ID) == null
                    && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).carriedCoat() == COAT;
        });
        if (phase == 1) {
            t.server("Kill the carrier while the Frenchie is packed", p -> p.kill(p.level()));
            t.awaitClient("Wait for the carrier's death screen", mc -> mc.screen instanceof DeathScreen);
            t.client("Request vanilla respawn", mc -> mc.player.respawn());
            t.awaitServer("Verify respawn retained the same packed dog", p -> p.isAlive()
                    && p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).frenchieNBT()
                    .read("UUID", UUIDUtil.CODEC).filter(DOG_ID::equals).isPresent());
            t.server("Return the carrier to the cleared plot", p ->
                    p.teleportTo(p.level().getServer().overworld(), 0.5, 201, 2.5, Set.of(), 180, 0, true));
            t.server("Wear a red front pack before saving", p -> p.setItemSlot(EquipmentSlot.CHEST, dyedPack(DyeColor.RED)));
            return;
        }
        t.awaitClient("Verify the red chest pack survived rejoin", mc ->
                hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.RED));
        framePlayer(t);
        t.screenshot("frontpack-dyed-chest");
        FrontpackCloseups.capture(t, "red");
        t.server("Hold a lime front pack with the red pack still equipped", p ->
                p.setItemInHand(InteractionHand.OFF_HAND, dyedPack(DyeColor.LIME)));
        t.awaitClient("Wait for the lime offhand pack", mc -> hasDye(mc.player.getOffhandItem(), DyeColor.LIME));
        framePlayer(t);
        t.screenshot("frontpack-dyed-offhand");
        FrontpackCloseups.capture(t, "lime");
        t.awaitClient("See dyed straps on the chest, over the shoulder and down the back",
                mc -> FrontpackCloseups.checkStraps(mc.gameDirectory.toPath().resolve("screenshots")));
        t.server("Wash the offhand pack at a water cauldron beside the player", p -> {
            var pos = p.blockPosition().east();
            var water = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2);
            p.level().setBlockAndUpdate(pos, water);
            p.gameMode.useItemOn(p, p.level(), p.getOffhandItem(), InteractionHand.OFF_HAND,
                    new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
            Playtest.require(p.level().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 1, "Wash did not use one water level");
        });
        t.awaitClient("Wait for the undyed offhand pack", mc -> mc.player.getOffhandItem().is(FrenchieRegistry.FRONT_PACK.get())
                && !mc.player.getOffhandItem().has(DataComponents.DYED_COLOR));
        framePlayer(t);
        t.screenshot("frontpack-washed");
        t.server("Stow both front packs", p -> {
            p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        });
        t.awaitClient("Wait for the original carrier", mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && mc.player.getOffhandItem().isEmpty());
        framePlayer(t);
        t.screenshot("frontpack-rejoined");
        t.awaitClient("Check red and lime fabric pixels returned to the original carrier color after washing",
                mc -> compareDyes(mc.gameDirectory.toPath().resolve("screenshots")));
        t.client("Crouch and aim at empty air", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.keyShift.setDown(true);
            mc.player.setXRot(-15);
            mc.player.setYRot(90);
        });
        t.awaitClient("Raycast empty air with an empty hand", FrontpackPlaytest::aimingAtAir);
        t.client("Right-click the air to set the dog down", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        t.awaitServer("Verify the only dog has the same UUID, owner and coat after restart", FrontpackPlaytest::isRestored);
        t.awaitClient("Check the pack disappeared and the restored dog synchronized", mc ->
                !mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()
                        && mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog && dog.isOwnedBy(mc.player) && dog.getVariant() == COAT);
        t.client("Turn away from the released dog", mc -> mc.player.setYRot(0));
        t.awaitClient("Verify the second click will also hit empty air", FrontpackPlaytest::aimingAtAir);
        t.client("Right-click empty air a second time", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        framePlayer(t);
        t.screenshot("frontpack-empty");
        FrontpackCloseups.checkEarsAfterRelease(t);
        t.awaitClient("Check the dark carrier was visible on the chest only while carrying", mc ->
                compareChest(mc.gameDirectory.toPath().resolve("screenshots")));
        t.server("Verify the repeat click left exactly one Frenchie", p -> Playtest.require(isRestored(p), "Dog was duplicated or lost"));
    }

    private static boolean aimingAtAir(Minecraft mc) {
        return mc.player.isShiftKeyDown() && mc.player.getMainHandItem().isEmpty()
                && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.MISS;
    }

    private static ItemStack dyedPack(DyeColor dye) {
        var stack = new ItemStack(FrenchieRegistry.FRONT_PACK.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF));
        return stack;
    }

    private static boolean hasDye(ItemStack pack, DyeColor color) {
        return pack.is(FrenchieRegistry.FRONT_PACK.get())
                && DyedItemColor.getOrDefault(pack, 0) == color.getTextureDiffuseColor();
    }

    private static boolean compareDyes(Path screenshots) {
        BufferedImage chest, offhand, washed, original;
        try {
            chest = ImageIO.read(screenshots.resolve("frontpack-dyed-chest.png").toFile());
            offhand = ImageIO.read(screenshots.resolve("frontpack-dyed-offhand.png").toFile());
            washed = ImageIO.read(screenshots.resolve("frontpack-washed.png").toFile());
            original = ImageIO.read(screenshots.resolve("frontpack-rejoined.png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (chest == null || offhand == null || washed == null || original == null) return false;
        int w = chest.getWidth(), h = chest.getHeight();
        int changed = 0;
        // Restrict the sample to the torso so a tinted inventory/held icon cannot pass.
        for (int y = h * 55 / 100; y < h * 66 / 100; y++) {
            for (int x = w / 2 - h / 30; x < w / 2 + h / 30; x++) {
                int redFrame = chest.getRGB(x, y), greenFrame = offhand.getRGB(x, y);
                int plain = original.getRGB(x, y), clean = washed.getRGB(x, y);
                if (channel(redFrame, 16) > channel(redFrame, 8) + 12
                        && channel(greenFrame, 8) > channel(greenFrame, 16) + 12
                        && channel(plain, 16) < 85
                        && Math.abs(channel(plain, 16) - channel(clean, 16)) < 10
                        && Math.abs(channel(plain, 8) - channel(clean, 8)) < 10
                        && Math.abs(channel(plain, 0) - channel(clean, 0)) < 10) changed++;
            }
        }
        Playtest.require(changed > Math.max(10, h * h / 6000), "Too few chest pixels went from red to lime to clean: " + changed);
        return true;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 255;
    }

    private static int dogCount(ServerPlayer p) {
        return p.level().getEntitiesOfClass(FrenchieEntity.class, TEST_AREA).size();
    }

    private static boolean isRestored(ServerPlayer p) {
        return !p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()
                && p.level().getEntity(DOG_ID) instanceof FrenchieEntity dog && dog.isOwnedBy(p)
                && dog.getVariant() == COAT && dog.getBoundingBox().intersects(TEST_AREA) && dogCount(p) == 1;
    }

    private static void framePlayer(Playtest t) {
        int[] settle = new int[1];
        t.client("Frame the standing player's chest from the front", mc -> {
            mc.options.keyShift.setDown(false);
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(0);
            mc.player.setYRot(180);
            mc.player.yBodyRot = 180;
            settle[0] = mc.player.tickCount + 16;
        });
        t.awaitClient("Wait for the camera and standing pose to settle", mc -> !mc.player.isCrouching() && mc.player.tickCount >= settle[0]);
    }

    private static boolean compareChest(Path screenshots) {
        BufferedImage packed, empty;
        try {
            packed = ImageIO.read(screenshots.resolve("frontpack-rejoined.png").toFile());
            empty = ImageIO.read(screenshots.resolve("frontpack-empty.png").toFile());
        } catch (IOException e) {
            return false; // The screenshot writer may still be encoding the last frame.
        }
        if (packed == null || empty == null) return false;
        int w = packed.getWidth(), h = packed.getHeight();
        Playtest.require(w == empty.getWidth() && h == empty.getHeight(), "Viewport changed during frontpack test");
        int carrierPixels = 0;
        // At the default FOV the front-facing avatar's chest is just below the center of the frame.
        for (int y = h * 55 / 100; y < h * 65 / 100; y++) {
            for (int x = w / 2 - h / 30; x < w / 2 + h / 30; x++) {
                int a = packed.getRGB(x, y), b = empty.getRGB(x, y);
                int r = a >> 16 & 255, g = a >> 8 & 255, blue = a & 255;
                if (r < 85 && Math.abs(r - g) < 12 && Math.abs(r - blue) < 12
                        && Math.abs(r - (b >> 16 & 255)) + Math.abs(g - (b >> 8 & 255)) + Math.abs(blue - (b & 255)) > 60) {
                    carrierPixels++;
                }
            }
        }
        Playtest.require(carrierPixels > Math.max(10, h * h / 5000), "Expected dark carrier on chest, changed pixels=" + carrierPixels);
        return true;
    }
}
