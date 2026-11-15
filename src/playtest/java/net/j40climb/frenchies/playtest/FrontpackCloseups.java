package net.j40climb.frenchies.playtest;

import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

final class FrontpackCloseups {
    static void capture(Playtest t, String dye) {
        // The player faces north throughout. An independent camera keeps the head and shoulders still.
        front(t, dye);
        photograph(t, dye, "side", new Vec3(1.27, 1.45, -0.56), 1.23);
        photograph(t, dye, "shoulders", new Vec3(0.88, 2.25, 1.04), 1.36);
        photograph(t, dye, "back", new Vec3(0, 1.22, 1.45), 1.22);
        restoreCamera(t);
    }

    private static void front(Playtest t, String label) {
        photograph(t, label, "front", new Vec3(0, 1.32, -1.35), 1.20);
    }

    private static void restoreCamera(Playtest t) {
        t.client("Return the camera to the player", mc -> {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
    }

    static void checkEarsAfterRelease(Playtest t) {
        front(t, "empty");
        restoreCamera(t);
        t.awaitClient("See both ears above the carried dog's head", mc -> {
            var shots = mc.gameDirectory.toPath().resolve("screenshots");
            BufferedImage dog, empty;
            try {
                dog = ImageIO.read(shots.resolve("frontpack-red-front.png").toFile());
                empty = ImageIO.read(shots.resolve("frontpack-empty-front.png").toFile());
            } catch (IOException e) {
                return false;
            }
            if (dog == null || empty == null) return false;
            int h = dog.getHeight(), center = dog.getWidth() / 2;
            // Each ear breaks the silhouette above the flat top of the dog's head.
            for (int side : new int[]{-1, 1}) {
                int changed = 0;
                for (int y = h * 31 / 100; y < h * 37 / 100; y++) {
                    for (int dx = h / 20; dx < h * 15 / 100; dx++) {
                        int x = center + side * dx;
                        int a = dog.getRGB(x, y), b = empty.getRGB(x, y);
                        if (((a >> 8) & 255) - ((b >> 8) & 255) > 30
                                && (a & 255) - (b & 255) > 30) changed++;
                    }
                }
                Playtest.require(changed >= h * h / 2000, "Missing raised ear on side " + side + ": " + changed + " pixels");
            }
            return true;
        });
    }

    private static void photograph(Playtest t, String dye, String angle, Vec3 offset, double targetHeight) {
        t.client("Position the " + angle + " close-up camera", mc -> {
            var position = mc.player.position().add(offset);
            var aim = mc.player.position().add(0, targetHeight, 0).subtract(position);
            var marker = new Marker(EntityType.MARKER, mc.level);
            float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
            marker.snapTo(position.add(0, -marker.getEyeHeight(), 0), yaw, pitch);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(marker);
        });
        t.awaitClient("Wait for the " + angle + " camera to reach its mark", mc ->
                mc.gameRenderer.getMainCamera().position().distanceToSqr(mc.player.position().add(offset)) < 0.00001);
        t.screenshot("frontpack-" + dye + "-" + angle);
    }

    static boolean checkStraps(Path screenshots) {
        try {
            return checkView(screenshots, "front") && checkView(screenshots, "shoulders") && checkView(screenshots, "back");
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean checkView(Path screenshots, String angle) throws IOException {
        var red = ImageIO.read(screenshots.resolve("frontpack-red-" + angle + ".png").toFile());
        var lime = ImageIO.read(screenshots.resolve("frontpack-lime-" + angle + ".png").toFile());
        if (red == null || lime == null) return false;
        int h = red.getHeight(), center = red.getWidth() / 2;
        if (angle.equals("shoulders")) {
            // The raised camera sees the near sleeve's top to the right of the head.
            int changed = 0;
            for (int y = h * 48 / 100; y < h * 58 / 100; y++) {
                for (int x = center + h * 5 / 100; x < center + h * 17 / 100; x++) {
                    if (changesDye(red, lime, x, y)) changed++;
                }
            }
            Playtest.require(changed >= h * h / 5000, "Dyed strap missing from the shoulder top: " + changed + " pixels");
            return true;
        }
        // Count dyed rows beside the dog's head in front or above the waist belt behind.
        boolean front = angle.equals("front");
        for (int side : new int[]{-1, 1}) {
            int dyedRows = 0;
            for (int y = h * (front ? 40 : 42) / 100; y < h * (front ? 52 : 55) / 100; y++) {
                int width = 0;
                for (int dx = h * (front ? 7 : 2) / 100; dx < h / 5; dx++) {
                    if (changesDye(red, lime, center + side * dx, y)) width++;
                }
                if (width >= Math.max(2, h / 180)) dyedRows++;
            }
            Playtest.require(dyedRows >= h * (front ? 4 : 8) / 100,
                    "Too little dyed strap in the " + angle + " view on side " + side + ": " + dyedRows + " rows");
        }
        return true;
    }

    private static boolean changesDye(BufferedImage red, BufferedImage lime, int x, int y) {
        int r = red.getRGB(x, y), g = lime.getRGB(x, y);
        return ((r >> 16) & 255) - ((r >> 8) & 255) > 15 && ((g >> 8) & 255) - ((g >> 16) & 255) > 15;
    }
}
