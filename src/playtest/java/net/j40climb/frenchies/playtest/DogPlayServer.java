package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayPose;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class DogPlayServer {
    static final UUID DOG = UUID.fromString("1971aa10-7b69-4584-958f-cac32867c31a");
    static final UUID SLIME = UUID.fromString("7b7a49f5-7ae0-467b-af7f-84653236315c");
    static final String BLOW = "Play fixture: blow bubbles";
    static final String WATCH_BUBBLES = "Play fixture: watch bubbles";
    static final String WATCH_BOW = "Play fixture: watch bow";
    static final String WATCH_RELOAD = "Play fixture: watch restored dog";
    static final String BLOW_AGAIN = "Play fixture: blow again";
    private static final BlockPos YARD = new BlockPos(480, 220, 480);
    private enum Step { JOIN, BUBBLES, BOW, RELOAD, RECRUIT, AWAY, RETURN, TARGET }
    private static Step step = Step.JOIN;

    static boolean finished(ServerPlayer owner, ServerPlayer visitor, Path root, int phase) {
        var level = owner.level();
        var dog = (FrenchieEntity) level.getEntity(DOG);
        switch (step) {
            case JOIN -> {
                require(!owner.getUUID().equals(visitor.getUUID()), "Expected distinct observers");
                if (phase == 2) {
                    cue(owner, WATCH_RELOAD);
                    cue(visitor, WATCH_RELOAD);
                    advance(Step.RELOAD);
                    break;
                }
                level.getServer().setDifficulty(Difficulty.NORMAL, true);
                level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
                for (var pos : BlockPos.betweenClosed(YARD.offset(-9, -1, -9), YARD.offset(9, -1, 9))) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                placePlayers(owner, visitor);
                owner.getInventory().clearContent();
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FrenchieRegistry.BUBBLE_TOY.get()));
                dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                require(dog != null, "Could not create Frenchie");
                dog.setUUID(DOG);
                dog.setPersistenceRequired();
                dog.snapTo(YARD, 180, 0);
                require(level.addFreshEntity(dog), "Could not add Frenchie");
                cue(owner, BLOW);
                cue(visitor, WATCH_BUBBLES);
                advance(Step.BUBBLES);
            }
            case BUBBLES -> {
                if (!frames(root, "bubbles") || dog.bubblePlay().engaged()) break;
                require(owner.getMainHandItem().is(FrenchieRegistry.BUBBLE_TOY.get()), "Blowing consumed the toy");
                dog.snapTo(YARD.west(3), -90, 0);
                var slime = EntityTypes.SLIME.create(level, EntitySpawnReason.COMMAND);
                require(slime != null, "Could not create slime");
                slime.setUUID(SLIME);
                slime.setSize(1, true);
                slime.setNoAi(true);
                slime.setPersistenceRequired();
                slime.snapTo(YARD, 0, 0);
                cue(owner, WATCH_BOW);
                cue(visitor, WATCH_BOW);
                require(level.addFreshEntity(slime), "Could not add slime");
                advance(Step.BOW);
            }
            case BOW -> {
                if (!frames(root, "slime-bow") || dog.playPose() == PlayPose.NONE) break;
                // Freeze during play. Remove the stimulus so a new session cannot mask
                // an incorrectly persisted pose when the two clients reconnect.
                level.getServer().tickRateManager().setFrozen(true);
                var slime = level.getEntity(SLIME);
                require(slime != null, "Frenchie harmed its playmate");
                slime.discard();
                require(level.getServer().saveEverything(false, true, true), "Failed to save during slime play");
                LogUtils.getLogger().info("Saved dog in pose {}", dog.playPose());
                return true;
            }
            case RELOAD -> {
                if (dog == null || !frames(root, "restored")) break;
                require(dog.playPose() == PlayPose.NONE && !dog.bubblePlay().engaged()
                        && level.getEntity(SLIME) == null, "Restart retained the missing play target");
                placePlayers(owner, visitor);
                dog.snapTo(YARD, 180, 0);
                cue(owner, BLOW_AGAIN);
                advance(Step.RECRUIT);
            }
            case RECRUIT -> {
                if (!dog.bubblePlay().engaged()) break;
                // This wild persistent dog stays in the yard without an owner to follow.
                for (var player : new ServerPlayer[]{owner, visitor}) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(level, YARD.getX() + 512, YARD.getY(), YARD.getZ() + 512, Set.of(), 0, 0, true);
                }
                advance(Step.AWAY);
            }
            case AWAY -> {
                if (dog != null || level.hasChunkAt(YARD)) break;
                placePlayers(owner, visitor);
                advance(Step.RETURN);
            }
            case RETURN -> {
                if (dog == null) break;
                require(dog.playPose() == PlayPose.NONE && !dog.bubblePlay().engaged()
                                && level.getEntitiesOfClass(PlayBubbleEntity.class, new AABB(YARD).inflate(12)).isEmpty(),
                        "Returning to unloaded terrain restored dangling bubble play");
                visitor.setGameMode(GameType.SURVIVAL);
                visitor.teleportTo(level, YARD.getX() + 3.7, YARD.getY(), YARD.getZ() + 0.5, Set.of(), 90, 0, true);
                var slime = EntityTypes.SLIME.create(level, EntitySpawnReason.COMMAND);
                require(slime != null, "Could not create live vanilla slime");
                slime.setSize(1, true);
                slime.setUUID(SLIME);
                slime.snapTo(YARD, 0, 0);
                require(level.addFreshEntity(slime), "Could not spawn vanilla slime");
                advance(Step.TARGET);
            }
            case TARGET -> {
                var slime = (Slime) level.getEntity(SLIME);
                require(slime != null && slime.isAlive(), "Frenchie harmed the vanilla slime");
                return slime.getTarget() == visitor && slime.distanceToSqr(visitor) < 4;
            }
        }
        return false;
    }

    private static void placePlayers(ServerPlayer owner, ServerPlayer visitor) {
        owner.setGameMode(GameType.CREATIVE);
        owner.teleportTo(owner.level(), YARD.getX() + 0.5, YARD.getY(), YARD.getZ() - 3.0, Set.of(), 0, 12, true);
        visitor.setGameMode(GameType.SPECTATOR);
        visitor.teleportTo(visitor.level(), YARD.getX() + 3.0, YARD.getY() - 0.65, YARD.getZ() - 1.4, Set.of(), 0, 0, true);
    }

    private static void cue(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static boolean frames(Path root, String name) {
        return Files.isRegularFile(root.resolve("Owner/screenshots/owner-" + name + ".png"))
                && Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-" + name + ".png"));
    }

    private static void advance(Step next) {
        step = next;
        LogUtils.getLogger().info("Play fixture reached {}", next);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
