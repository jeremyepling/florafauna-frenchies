package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** Drive the local player's ordinary boat inputs and capture their main framebuffer. */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class CaptainViewClient {
    private enum Step { WAIT, BOARD, PORTRAIT, BROADSIDE, REAR, BASELINE, SOLO, STRAIGHT, GLANCE, RIGHT, LEFT, LEAVE }
    private static Step step = Step.WAIT;
    private static int ticks;
    private static float previousHeading;

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        switch (event.getMessage().getString()) {
            case "FF rowing board" -> step = Step.BOARD;
            case "FF rowing portrait" -> step = Step.PORTRAIT;
            case "FF rowing broadside" -> step = Step.BROADSIDE;
            case "FF rowing rear view" -> step = Step.REAR;
            case "FF rowing baseline" -> step = Step.BASELINE;
            case "FF rowing solo" -> step = Step.SOLO;
            case "FF rowing row" -> step = Step.STRAIGHT;
            case "FF rowing leave" -> step = Step.LEAVE;
            default -> { return; }
        }
        ticks = 0;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        switch (step) {
            case BASELINE, SOLO, STRAIGHT, GLANCE, RIGHT, LEFT ->
                    event.getInput().keyPresses = new Input(true, false, step == Step.LEFT, step == Step.RIGHT, false, false, false);
            case LEAVE -> event.getInput().keyPresses = new Input(false, false, false, false, false, true, false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (step == Step.WAIT) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
        if (!(mc.level.getEntity(CaptainCourse.BOAT) instanceof AbstractBoat boat)) return;
        ++ticks;
        if (step == Step.BOARD) {
            mc.player.lookAt(Anchor.EYES, boat.getBoundingBox().getCenter());
            if (ticks == 5 || (ticks % 15 == 0 && !mc.player.isPassenger())) {
                mc.gameMode.interact(mc.player, boat, new EntityHitResult(boat, boat.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
            }
            if (mc.player.getVehicle() == boat) step = Step.WAIT;
            return;
        }
        if (step == Step.PORTRAIT || step == Step.BROADSIDE) {
            if (!(mc.level.getEntity(CompanionServer.CAPTAIN) instanceof FrenchieEntity dog)
                    || dog.getVehicle() != boat || !dog.hasFlotationVest()) return;
            boolean broadside = step == Step.BROADSIDE;
            if (broadside && !mc.player.isSpectator()) return;
            mc.player.lookAt(Anchor.EYES, broadside ? boat.position().add(0, 0.45, 0) : dog.getEyePosition());
            if (ticks < 10) return;
            capture(mc, broadside ? "dockside-broadside" : "captain");
            step = Step.WAIT;
            return;
        }
        if (step == Step.REAR) {
            if (mc.player.getVehicle() != boat || boat.getControllingPassenger() == mc.player
                    || !(boat.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player)
                    || !(mc.level.getEntity(CompanionServer.CAPTAIN) instanceof FrenchieEntity dog)
                    || dog.getVehicle() != boat) return;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.getToastManager().clear();
            mc.player.setYRot(boat.getYRot());
            mc.player.setXRot(4);
            if (ticks < 8 || !boat.getPaddleState(0) || !boat.getPaddleState(1)) return;
            capture(mc, "rear-passenger-forward");
            step = Step.WAIT;
            return;
        }
        if (step == Step.LEAVE) {
            if (!mc.player.isPassenger()) step = Step.WAIT;
            return;
        }
        if (boat.getControllingPassenger() != mc.player) return;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.hideGui = false;
        mc.getToastManager().clear();
        // A driver looking along the bow, with the horizon a few degrees above the crosshair.
        mc.player.setYRot(boat.getYRot());
        mc.player.setXRot(step == Step.GLANCE ? 23 : 4);
        if (!boat.getPaddleState(0) || !boat.getPaddleState(1)) return;
        if (step != Step.BASELINE) {
            if (!(mc.level.getEntity(CompanionServer.CAPTAIN) instanceof FrenchieEntity dog)
                    || dog.getVehicle() != boat || !dog.hasFlotationVest()) return;
        }
        switch (step) {
            case BASELINE, SOLO, STRAIGHT -> {
                if (ticks < 12) return;
                String frame = switch (step) {
                    case BASELINE -> "rowing-baseline";
                    case SOLO -> "rowing-solo";
                    default -> "rowing-straight";
                };
                capture(mc, frame);
                previousHeading = boat.getYRot();
                step = step == Step.STRAIGHT ? Step.GLANCE : Step.WAIT;
            }
            case GLANCE -> {
                if (ticks < 6) return;
                capture(mc, "rowing-glance");
                step = Step.RIGHT;
            }
            case RIGHT -> {
                if (ticks < 5 || Mth.wrapDegrees(boat.getYRot() - previousHeading) < 9) return;
                capture(mc, "rowing-right");
                previousHeading = boat.getYRot();
                step = Step.LEFT;
            }
            case LEFT -> {
                if (ticks < 8 || Mth.wrapDegrees(boat.getYRot() - previousHeading) > -10) return;
                capture(mc, "rowing-left");
                step = Step.WAIT;
            }
        }
        ticks = 0;
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, ignored -> {});
    }
}
