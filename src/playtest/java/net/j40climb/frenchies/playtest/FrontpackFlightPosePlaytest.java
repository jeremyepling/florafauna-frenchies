package net.j40climb.frenchies.playtest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;

final class FrontpackFlightPosePlaytest {
    static void capture(Playtest t) {
        Pose[] samples = new Pose[3];
        // Opposite glances also sample the retracted and extended extremes of the pant.
        sample(t, "right", 21, samples, 0);
        sample(t, "center", 48, samples, 1);
        sample(t, "left", 75, samples, 2);
        t.awaitClient("See a forward-looking head, opposite glances and a continuously visible pant",
                mc -> check(mc.gameDirectory.toPath().resolve("screenshots"), samples));
    }

    private static void sample(Playtest t, String label, int tick, Pose[] samples, int index) {
        t.awaitClient("Follow the Frenchie until its " + label + " flight pose", mc -> {
            followFace(mc);
            if (!mc.player.isFallFlying() || mc.player.getFallFlyingTicks() % 96 != tick) return false;
            samples[index] = pose(mc);
            return true;
        });
        t.screenshot("frontpack-flight-" + label, FrontpackFlightPosePlaytest::followFace);
    }

    private static void followFace(Minecraft mc) {
        FrontpackExpressionPlaytest.faceCamera(mc);
        var target = mc.player.position().add(0, 0.23, -0.4);
        var position = target.add(0, -0.64, -2.2);
        var camera = mc.getCameraEntity();
        camera.snapTo(position.add(0, -camera.getEyeHeight(), 0), 0,
                (float) -Math.toDegrees(Math.atan2(target.y - position.y, target.z - position.z)));
    }

    private record Pose(Vector3f muzzle, float tongueExtension) {}

    private static Pose pose(Minecraft mc) {
        var renderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var state = (AvatarRenderState) renderer.createRenderState(mc.player, 0);
        var part = mc.getEntityModels().bakeLayer(FrontpackModel.LAYER_LOCATION);
        new FrontpackModel(part).setupAnim(state);
        var transforms = new PoseStack();
        for (String child : new String[]{"root", "carried_dog", "frenchie", "body", "head"}) {
            part = part.getChild(child);
            part.translateAndRotate(transforms);
        }
        // The player's torso pitches forward 90 degrees in a level glide. The rig's upward
        // direction (-Y) becomes forward in world space; the muzzle points down its local -Z.
        var muzzle = transforms.last().pose().transformDirection(new Vector3f(0, 0, -1)).normalize();
        var tongue = part.getChild("tongue");
        Playtest.require(tongue.visible, "The pant hid the tongue while gliding");
        return new Pose(muzzle, -tongue.z);
    }

    private static boolean check(Path shots, Pose[] samples) {
        var right = samples[0];
        var center = samples[1];
        var left = samples[2];
        Playtest.require(center.muzzle.y < -0.85 && Math.abs(center.muzzle.x) < 0.12
                        && right.muzzle.x < -0.20 && left.muzzle.x > 0.20
                        && right.muzzle.y < -0.70 && left.muzzle.y < -0.70,
                "Expected the muzzle to face the flight path and glance to each side: "
                        + right.muzzle + ", " + center.muzzle + ", " + left.muzzle);
        Playtest.require(left.tongueExtension > right.tongueExtension + 0.30, "The tongue did not extend through the pant");
        try {
            for (String label : new String[]{"right", "center", "left"}) {
                var image = ImageIO.read(shots.resolve("frontpack-flight-" + label + ".png").toFile());
                if (image == null) return false;
                int h = image.getHeight(), cx = image.getWidth() / 2, pink = 0;
                for (int y = h / 9; y < h * 8 / 9; y++) {
                    for (int x = cx - h / 3; x < cx + h / 3; x++) {
                        int rgb = image.getRGB(x, y);
                        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                        if (r > g + 30 && b > g + 4 && r > b + 28) pink++;
                    }
                }
                Playtest.require(pink >= h * h / 15000, "No visible pink tongue in the " + label + " frame: " + pink);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
