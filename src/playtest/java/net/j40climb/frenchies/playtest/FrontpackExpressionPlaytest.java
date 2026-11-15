package net.j40climb.frenchies.playtest;

import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

final class FrontpackExpressionPlaytest {
    static void waitForRest(Playtest t) {
        waitForPhase(t, "Wait for the resting head pose", 60, 195);
    }

    static void capture(Playtest t) {
        // The close-up camera stays fixed throughout all three poses.
        waitForPhase(t, "Wait for the carried Frenchie's head tilt", 21, 23);
        t.screenshot("frontpack-expressive-front", FrontpackExpressionPlaytest::faceCamera);
        waitForRest(t);
        t.screenshot("frontpack-restored-front", FrontpackExpressionPlaytest::faceCamera);
        t.awaitClient("See the ears tilt and the pink tongue appear, then retract", mc ->
                check(mc.gameDirectory.toPath().resolve("screenshots")));
    }

    private static void waitForPhase(Playtest t, String label, int start, int end) {
        t.awaitClient(label, mc -> {
            faceCamera(mc);
            int phase = Math.floorMod(mc.player.tickCount + mc.player.getId() * 37, 220);
            return phase >= start && phase <= end;
        });
    }

    static void faceCamera(Minecraft mc) {
        var player = mc.player;
        player.setYRot(180);
        player.setXRot(0);
        player.yRotO = player.yBodyRot = player.yBodyRotO = player.yHeadRot = player.yHeadRotO = 180;
        player.xRotO = 0;
    }

    private static boolean check(Path images) {
        try {
            var rest = ImageIO.read(images.resolve("frontpack-red-front.png").toFile());
            var tilt = ImageIO.read(images.resolve("frontpack-expressive-front.png").toFile());
            var relaxed = ImageIO.read(images.resolve("frontpack-restored-front.png").toFile());
            if (rest == null || tilt == null || relaxed == null) return false;
            int h = rest.getHeight(), restingSlope = earSlope(rest), tiltedSlope = earSlope(tilt);
            Playtest.require(Math.abs(tiltedSlope - restingSlope) >= h / 55 && Math.abs(earSlope(relaxed) - restingSlope) <= h / 100,
                    "Expected the ears to tilt then level out; resting slope=" + restingSlope + ", tilted slope=" + tiltedSlope);
            int out = tonguePixels(tilt), before = tonguePixels(rest), after = tonguePixels(relaxed);
            Playtest.require(out >= h * h / 4000 && before < out / 8 && after < out / 8,
                    "Pink tongue pixels before, during, after the tilt: " + before + ", " + out + ", " + after);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int earSlope(BufferedImage image) {
        int h = image.getHeight(), center = image.getWidth() / 2, slope = 0;
        for (int side : new int[]{-1, 1}) {
            int totalY = 0, count = 0;
            for (int y = h / 4; y < h * 44 / 100; y++) {
                for (int dx = h / 30; dx < h / 6; dx++) {
                    int rgb = image.getRGB(center + side * dx, y);
                    int red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                    if (red > green + 5 && green > 110 && blue > 100 && green - blue < 32) {
                        count++;
                        totalY += y;
                    }
                }
            }
            Playtest.require(count > h * h / 2500, "Could not locate the raised ear on side " + side);
            slope += side * (totalY / count);
        }
        return slope;
    }

    private static int tonguePixels(BufferedImage image) {
        int h = image.getHeight(), center = image.getWidth() / 2, pink = 0;
        for (int y = h * 45 / 100; y < h * 63 / 100; y++) {
            for (int x = center - h / 10; x <= center + h / 10; x++) {
                int rgb = image.getRGB(x, y);
                int red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                if (red > green + 30 && blue > green + 4) pink++;
            }
        }
        return pink;
    }
}
