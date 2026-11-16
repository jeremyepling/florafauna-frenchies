package net.j40climb.frenchies.playtest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** A fresh driver boards the same model boat on the same heading for both first-person passes. */
final class CaptainCourse {
    static final UUID BOAT = UUID.fromString("4699ca0b-a146-4322-bad2-d35f331722d5");
    private enum Step { BASELINE, RESET, DRIVER, SOLO, PORTRAIT, BROADSIDE, GESTURE, PASSENGER, ROW, EXIT }
    private static Step step;
    private static BlockPos launch;
    private static Path output;
    private static Path guestFrames;
    private static BlockPos boardingFloat;

    static void begin(ServerPlayer owner, ServerPlayer guest, Path root) {
        output = root.resolve("Owner/screenshots");
        guestFrames = root.resolve("Visitor/screenshots");
        var level = owner.level();
        // Align the course with the natural flat-world horizon, away from the elevated test yard.
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, 48, 0) - 1;
        launch = new BlockPos(48, y, 0);
        for (int x = -12; x <= 12; x++) {
            for (int z = -2; z <= 28; z++) {
                var pos = launch.offset(x, 0, z);
                boolean shore = Math.abs(x) == 12 || z == -2 || z == 28;
                level.setBlockAndUpdate(pos, (shore ? Blocks.OAK_PLANKS : Blocks.WATER).defaultBlockState());
                if (!shore) level.setBlockAndUpdate(pos.below(), Blocks.SAND.defaultBlockState());
            }
        }
        // Striped channel markers make any occlusion or change of view direction easy to see.
        for (int z : new int[] {8, 17, 26}) {
            for (int x : new int[] {-9, 9}) {
                var pos = launch.offset(x, 1, z);
                for (int height = 0; height < 4; height++) {
                    var block = height % 2 == 0 ? Blocks.WHITE_CONCRETE : x < 0 ? Blocks.RED_CONCRETE : Blocks.BLUE_CONCRETE;
                    level.setBlockAndUpdate(pos.above(height), block.defaultBlockState());
                }
                level.setBlockAndUpdate(pos.above(4), Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        position(owner, -1);
        position(guest, 1);
        spawnBoat(level);
        message(owner, "board");
        step = Step.BASELINE;
    }

    static boolean finished(ServerPlayer owner, ServerPlayer guest) {
        var level = owner.level();
        var boat = (AbstractBoat) level.getEntity(BOAT);
        var dog = (FrenchieEntity) level.getEntity(CompanionServer.CAPTAIN);
        switch (step) {
            case BASELINE -> {
                if (owner.getVehicle() != boat) return false;
                message(owner, "baseline");
                step = Step.RESET;
            }
            case RESET -> {
                if (!Files.isRegularFile(output.resolve("rowing-baseline.png"))) return false;
                owner.stopRiding();
                boat.discard();
                position(owner, -1);
                position(guest, 1);
                spawnBoat(level);
                CompanionServer.spawn(owner, launch.offset(0, 1, -2), CompanionServer.CAPTAIN).standUp();
                message(owner, "board");
                step = Step.DRIVER;
            }
            case DRIVER -> {
                if (boat.getControllingPassenger() != owner || dog.getVehicle() != boat || !dog.hasFlotationVest()) return false;
                message(owner, "solo");
                step = Step.SOLO;
            }
            case SOLO -> {
                if (!Files.isRegularFile(output.resolve("rowing-solo.png"))) return false;
                require(!guest.isPassenger() && boat.getControllingPassenger() == owner && dog.getVehicle() == boat,
                        "Solo frame needs the driver and captain aboard");
                // A small boarding float lets the guest photograph the captain and take the rear seat.
                boardingFloat = BlockPos.containing(boat.getX() + 1.8, launch.getY(), boat.getZ() + 2.4);
                level.setBlockAndUpdate(boardingFloat, Blocks.OAK_PLANKS.defaultBlockState());
                guest.teleportTo(level, boardingFloat.getX() + 0.5, boardingFloat.getY() + 1, boardingFloat.getZ() + 0.5,
                        Set.of(), 135, 20, true);
                message(guest, "portrait");
                step = Step.PORTRAIT;
            }
            case PORTRAIT -> {
                if (!Files.isRegularFile(guestFrames.resolve("captain.png"))) return false;
                // Put the observer's eye just above the gunwale for a view across the bow.
                double yaw = Math.toRadians(boat.getYRot());
                guest.setGameMode(GameType.SPECTATOR);
                guest.teleportTo(level, boat.getX() + 2.8 * Math.cos(yaw), boat.getY() - 0.9,
                        boat.getZ() + 2.8 * Math.sin(yaw), Set.of(), 90, 0, true);
                message(guest, "broadside");
                step = Step.BROADSIDE;
            }
            case BROADSIDE -> {
                if (!Files.isRegularFile(guestFrames.resolve("dockside-broadside.png"))) return false;
                message(guest, "gesture");
                step = Step.GESTURE;
            }
            case GESTURE -> {
                if (!Files.isRegularFile(guestFrames.resolve("captain-gesture-settled.png"))) return false;
                guest.setGameMode(GameType.SURVIVAL);
                guest.teleportTo(level, boardingFloat.getX() + 0.5, boardingFloat.getY() + 1, boardingFloat.getZ() + 0.5,
                        Set.of(), 135, 20, true);
                message(guest, "board");
                step = Step.PASSENGER;
            }
            case PASSENGER -> {
                if (guest.getVehicle() != boat) return false;
                require(boat.getControllingPassenger() == owner && dog.getVehicle() == boat, "Second player displaced the captain or driver");
                message(owner, "row");
                message(guest, "rear view");
                step = Step.ROW;
            }
            case ROW -> {
                if (!Files.isRegularFile(output.resolve("rowing-left.png"))
                        || !Files.isRegularFile(guestFrames.resolve("rear-passenger-forward.png"))) return false;
                require(Files.isRegularFile(output.resolve("rowing-right.png")) && Files.isRegularFile(output.resolve("rowing-straight.png"))
                                && Files.isRegularFile(output.resolve("rowing-glance.png")),
                        "Missing driver's turn or forward frame");
                require(boat.distanceToSqr(launch.getBottomCenter()) > 12 && dog.getVehicle() == boat
                        && guest.getVehicle() == boat && owner.getVehicle() == boat, "Passengers or forward motion lost while rowing");
                message(owner, "leave");
                step = Step.EXIT;
            }
            case EXIT -> {
                if (owner.isPassenger() || dog.isPassenger()) return false;
                require(guest.getVehicle() == boat && !dog.hasFlotationVest(), "Dismount affected the guest or left vest visible");
                return true;
            }
        }
        return false;
    }

    private static void position(ServerPlayer player, int x) {
        player.teleportTo(player.level(), launch.getX() + x + 0.5, launch.getY() + 1, launch.getZ() - 1.5, Set.of(), 0, 5, true);
    }

    private static void spawnBoat(ServerLevel level) {
        var boat = EntityType.OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
        require(boat != null, "Boat creation failed");
        boat.setUUID(BOAT);
        boat.snapTo(launch.getX() + 0.5, launch.getY() + 0.9, launch.getZ() + 0.5, 0, 0);
        require(level.addFreshEntity(boat), "Boat spawn failed");
    }

    private static void message(ServerPlayer player, String action) {
        player.sendSystemMessage(Component.literal("FF rowing " + action));
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
    }
}
