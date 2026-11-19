package net.j40climb.frenchies.playtest;

import net.minecraft.client.CameraType;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
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
        if (dye.equals("red")) {
            FrontpackExpressionPlaytest.capture(t);
            hem(t, dye);
        }
        photograph(t, dye, "side", new Vec3(1.35, 1.26, 0), 1.26);
        photograph(t, dye, "shoulders", new Vec3(0.88, 2.25, 1.04), 1.36);
        photograph(t, dye, "back", new Vec3(0, 1.22, 1.45), 1.22);
        restoreCamera(t);
    }

    private static void front(Playtest t, String label) {
        photograph(t, label, "front", new Vec3(0, 1.32, -1.35), 1.20);
    }

    static void wingedPack(Playtest t) {
        front(t, "winged");
        photograph(t, "winged", "back", new Vec3(0, 1.22, 1.45), 1.22);
        restoreCamera(t);
    }

    static void landed(Playtest t) {
        front(t, "landed");
        restoreCamera(t);
        t.awaitClient("Recover the resting forward face and retracted tongue after landing", mc ->
                FrontpackExpressionPlaytest.landed(mc.gameDirectory.toPath().resolve("screenshots")));
    }

    private static void hem(Playtest t, String label) {
        photograph(t, label, "hem", new Vec3(0, 0.50, -1.40), 0.86);
    }

    private static void restoreCamera(Playtest t) {
        t.client("Return the camera to the player", mc -> {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
    }

    static void checkEarsAfterRelease(Playtest t) {
        front(t, "empty");
        hem(t, "empty");
        restoreCamera(t);
        t.awaitClient("See a small centered tail below the hem with the rump inside the pouch",
                mc -> checkTail(mc.gameDirectory.toPath().resolve("screenshots")));
        t.awaitClient("See raised ears and verify the unobscured front straps meet the pouch inside its rim", mc -> {
            var shots = mc.gameDirectory.toPath().resolve("screenshots");
            BufferedImage dog, empty;
            try {
                dog = ImageIO.read(shots.resolve("frontpack-red-front.png").toFile());
                empty = ImageIO.read(shots.resolve("frontpack-empty-front.png").toFile());
            } catch (IOException e) {
                return false;
            }
            if (dog == null || empty == null) return false;
            checkStrapInset(empty);
            int h = dog.getHeight(), center = dog.getWidth() / 2;
            // Check the entire shoulder-to-pouch line once the dog's face is out of the way.
            checkStrapLines(h, center, true, (x, y) -> redFabric(empty.getRGB(x, y)));
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

    private static void checkStrapInset(BufferedImage empty) {
        int h = empty.getHeight(), center = empty.getWidth() / 2;
        for (int side : new int[]{-1, 1}) {
            int shoulder = 0, pouch = 0;
            for (int dx = 1; dx < h / 5; dx++) {
                int x = center + side * dx;
                if (redFabric(empty.getRGB(x, h * 46 / 100))) shoulder = dx;
                if (redFabric(empty.getRGB(x, h * 62 / 100))) pouch = dx;
            }
            // The pack front is closer to this fixed camera than the shoulder straps.
            // A strap aligned with the pouch edge projects at about 82% of its half-width.
            Playtest.require(shoulder > 0 && shoulder <= pouch * 86 / 100,
                    "Shoulder strap extends past the pouch rim on side " + side + ": " + shoulder + " vs " + pouch);
        }
    }

    private static boolean redFabric(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r > 51 && r > g * 1.9 && g - b < 13;
    }

    private static boolean checkTail(Path shots) {
        BufferedImage occupied, empty;
        try {
            occupied = ImageIO.read(shots.resolve("frontpack-red-hem.png").toFile());
            empty = ImageIO.read(shots.resolve("frontpack-empty-hem.png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (occupied == null || empty == null) return false;
        int h = empty.getHeight(), center = empty.getWidth() / 2;
        // From below, the shaded underside projects behind the tail. Find the lower edge
        // of the lit front face before measuring how much fur is visible beneath it.
        int strongestRed = 0, hem = 0;
        for (int y = h / 3; y < h * 3 / 4; y++) {
            int rgb = empty.getRGB(center, y);
            strongestRed = Math.max(strongestRed, (rgb >> 16 & 255) - Math.max(rgb >> 8 & 255, rgb & 255));
        }
        for (int y = h / 3; y < h * 3 / 4; y++) {
            int rgb = empty.getRGB(center, y);
            if ((rgb >> 16 & 255) - Math.max(rgb >> 8 & 255, rgb & 255) >= strongestRed * 3 / 4) hem = y;
        }
        Playtest.require(hem > 0 && strongestRed > 30, "Could not locate the red pouch hem");
        int tailPixels = 0, exposedRump = 0, depth = 0;
        // Sample between the hind paws. Only the narrow central nub should change the empty-pack silhouette.
        for (int dy = 1; dy <= h / 16; dy++) {
            for (int dx = -h / 14; dx <= h / 14; dx++) {
                int dog = occupied.getRGB(center + dx, hem + dy), background = empty.getRGB(center + dx, hem + dy);
                int difference = Math.abs((dog >> 16 & 255) - (background >> 16 & 255))
                        + Math.abs((dog >> 8 & 255) - (background >> 8 & 255)) + Math.abs((dog & 255) - (background & 255));
                if (difference < 30) continue;
                if (Math.abs(dx) <= h / 23) {
                    tailPixels++;
                    depth = Math.max(depth, dy);
                } else {
                    exposedRump++;
                }
            }
        }
        Playtest.require(tailPixels >= h * h / 3000 && depth <= h / 23 && exposedRump <= h * h / 8000,
                "Expected just the tail below the pouch: tail pixels=" + tailPixels + ", depth=" + depth + ", flank pixels=" + exposedRump);
        return true;
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
        t.awaitClient("Wait for the " + angle + " camera to reach its mark", mc -> {
            FrontpackExpressionPlaytest.faceCamera(mc);
            return mc.gameRenderer.getMainCamera().position().distanceToSqr(mc.player.position().add(offset)) < 0.00001;
        });
        if ((dye.equals("red") || dye.equals("landed")) && angle.equals("front")) FrontpackExpressionPlaytest.waitForRest(t);
        t.screenshot("frontpack-" + dye + "-" + angle, FrontpackExpressionPlaytest::faceCamera);
    }

    static boolean checkBackStraps(Path screenshots) {
        try {
            var red = ImageIO.read(screenshots.resolve("frontpack-red-back.png").toFile());
            var lime = ImageIO.read(screenshots.resolve("frontpack-lime-back.png").toFile());
            if (red == null || lime == null) return false;
            checkStrapLines(red.getHeight(), red.getWidth() / 2, false, (x, y) -> changesDye(red, lime, x, y));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private interface FabricMask {
        boolean contains(int x, int y);
    }

    private static void checkStrapLines(int h, int center, boolean front, FabricMask fabric) {
        String angle = front ? "front" : "back";
        int top = h * (front ? 40 : 42) / 100;
        int aboveJoin = h * (front ? 51 : 64) / 100;
        int belowJoin = h * (front ? 60 : 68) / 100;
        int inner = h / 50, outer = h / 5;
        for (int side : new int[]{-1, 1}) {
            int minEdge = h, maxEdge = 0;
            // The inside edge stays clear of the player's arms.
            for (int y = top; y <= aboveJoin; y++) {
                int edge = inner;
                while (edge <= outer && !fabric.contains(center + side * edge, y)) {
                    edge++;
                }
                minEdge = Math.min(minEdge, edge);
                maxEdge = Math.max(maxEdge, edge);
            }
            Playtest.require(maxEdge - minEdge <= h / (front ? 200 : 70), "Strap angles outward in the " + angle
                    + " view on side " + side + ": edge shifts " + (maxEdge - minEdge) + " pixels");

            // Follow a vertical strip of fabric from shoulder height into the pouch or waist belt.
            int columns = 0;
            for (int dx = inner; dx < outer; dx++) {
                int y = top;
                while (y <= belowJoin && fabric.contains(center + side * dx, y)) {
                    y++;
                }
                if (y > belowJoin) columns++;
            }
            Playtest.require(columns >= Math.max(2, h / (front ? 270 : 180)), "Strap does not run continuously into its attachment in the "
                    + angle + " view on side " + side + ": " + columns + " dyed columns");
        }
    }

    private static boolean changesDye(BufferedImage red, BufferedImage lime, int x, int y) {
        int r = red.getRGB(x, y), g = lime.getRGB(x, y);
        return ((r >> 16) & 255) - ((r >> 8) & 255) > 15 && ((g >> 8) & 255) - ((g >> 16) & 255) > 15;
    }
}
