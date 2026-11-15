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
        photograph(t, dye, "side", new Vec3(1.35, 1.26, 0), 1.26);
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
            return checkView(screenshots, "front") && checkView(screenshots, "back");
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean checkView(Path screenshots, String angle) throws IOException {
        boolean front = angle.equals("front");
        // The dog's muzzle occludes the straps in the close-up. The normal third-person
        // camera sees both outer edges all the way down to the pouch.
        String redFrame = front ? "frontpack-dyed-chest.png" : "frontpack-red-back.png";
        String limeFrame = front ? "frontpack-dyed-offhand.png" : "frontpack-lime-back.png";
        var red = ImageIO.read(screenshots.resolve(redFrame).toFile());
        var lime = ImageIO.read(screenshots.resolve(limeFrame).toFile());
        if (red == null || lime == null) return false;
        int h = red.getHeight(), center = red.getWidth() / 2;
        int top = h * (front ? 55 : 42) / 100;
        int aboveJoin = h * (front ? 58 : 64) / 100;
        int belowJoin = h * (front ? 60 : 68) / 100;
        int inner = h / 50, outer = h / (front ? 15 : 5);
        for (int side : new int[]{-1, 1}) {
            int minEdge = h, maxEdge = 0;
            // The front outer edge stays clear of the face. Behind, the inside edge stays clear of the arms.
            for (int y = top; y <= aboveJoin; y++) {
                int edge = front ? outer : inner;
                while (edge >= inner && edge <= outer && !changesDye(red, lime, center + side * edge, y)) {
                    edge += front ? -1 : 1;
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
                while (y <= belowJoin && changesDye(red, lime, center + side * dx, y)) {
                    y++;
                }
                if (y > belowJoin) columns++;
            }
            Playtest.require(columns >= Math.max(2, h / (front ? 270 : 180)), "Strap does not run continuously into its attachment in the "
                    + angle + " view on side " + side + ": " + columns + " dyed columns");
        }
        return true;
    }

    private static boolean changesDye(BufferedImage red, BufferedImage lime, int x, int y) {
        int r = red.getRGB(x, y), g = lime.getRGB(x, y);
        return ((r >> 16) & 255) - ((r >> 8) & 255) > 15 && ((g >> 8) & 255) - ((g >> 16) & 255) > 15;
    }
}
