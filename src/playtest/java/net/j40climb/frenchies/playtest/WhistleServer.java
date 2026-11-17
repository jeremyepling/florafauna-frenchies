package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class WhistleServer {
    static final UUID COURIER = UUID.fromString("e51a6476-d359-4824-a77f-bea2cf7bda26");
    static final UUID COMPANION = UUID.fromString("b628516c-8feb-4540-b75e-f3bc153ddd65");
    static final String DISMISS = "Whistle fixture: dismiss Violet";
    static final String CALL_ACTIVE = "Whistle fixture: call Pip";
    static final String CALL_COURIER = "Whistle fixture: call Violet";
    static final String SWING = "Whistle fixture: swing through Pip";
    static final String LOOK = "Whistle fixture: watch Pip";
    private static final BlockPos YARD = new BlockPos(480, 210, 480);
    private static final BlockPos INN = YARD.east(640);
    private enum Step { START, DISMISS, LEAVE, CALL, FOLLOW, SETTLE, PREPARE_PHOTO, PHOTO, TUMBLE, LINE_UP, THROUGH_DOG, ACCIDENT, SAVE, REJOIN, RESPAWN, LAST_CALL, RETURN_TO_KENNEL }
    private static Step step = Step.START;
    private static int ticks;
    private static Cow target;

    static boolean finished(ServerPlayer owner, ServerPlayer visitor, Path root, int phase) {
        var level = owner.level();
        var courier = (FrenchieEntity) level.getEntity(COURIER);
        var companion = (FrenchieEntity) level.getEntity(COMPANION);
        if (++ticks > 2000) throw new AssertionError("Whistle fixture timed out at " + step);
        switch (step) {
            case START -> {
                require(!owner.getUUID().equals(visitor.getUUID()), "Fixture requires distinct players");
                if (phase == 2) {
                    next(Step.REJOIN);
                    break;
                }
                level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
                platform(owner, YARD);
                platform(owner, INN);
                owner.setGameMode(GameType.SURVIVAL);
                visitor.setGameMode(GameType.SURVIVAL);
                place(owner, YARD.south(1));
                place(visitor, YARD.east(4));
                courier = spawn(owner, COURIER, "Violet", YARD);
                courier.setItemSlot(EquipmentSlot.FEET, named(new ItemStack(Items.LEATHER_BOOTS), "Violet's boots"));
                companion = spawn(owner, COMPANION, "Pip", YARD.west(3));
                companion.setItemSlot(EquipmentSlot.OFFHAND, named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Pip's squeaker"));
                owner.getInventory().clearContent();
                owner.setItemInHand(InteractionHand.MAIN_HAND, CourierParcel.parcel());
                owner.interactOn(courier, InteractionHand.MAIN_HAND, Vec3.ZERO);
                for (String action : new String[]{"choose", "send"}) {
                    command(owner, "frenchiedelivery " + COURIER + " " + action + " " + visitor.getStringUUID());
                }
                require(ItemStack.matches(courier.getParcel(), CourierParcel.parcel()) && owner.getMainHandItem().isEmpty(),
                        "Fixture failed to begin a real delivery");
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FrenchieRegistry.FRENCHIE_WHISTLE.get()));
                cue(owner, DISMISS);
                next(Step.DISMISS);
            }
            case DISMISS -> {
                // A courier can also disappear in transit. After a full delivery ETA the recipient must still have neither dog nor parcel.
                if (courier != null || ticks < 210) break;
                require(visitor.getInventory().countItem(Items.BUNDLE) == 0 && companion != null && companion.isOrderedToSit(),
                        "Dismissing a delivery affected the recipient or second dog");
                place(owner, INN);
                place(visitor, INN.east(3));
                next(Step.LEAVE);
            }
            case LEAVE -> {
                if (level.hasChunkAt(YARD) || companion != null) break;
                cue(owner, CALL_ACTIVE);
                next(Step.CALL);
            }
            case CALL -> {
                if (companion == null || companion.distanceToSqr(owner) >= 36) break;
                require(!companion.isOrderedToSit(), "Explicit recall failed to override the stay order");
                checkPip(companion);
                place(owner, YARD);
                place(visitor, YARD.east(3));
                next(Step.FOLLOW);
            }
            case FOLLOW -> {
                if (companion == null || companion.distanceToSqr(owner) >= 36) break;
                require(courier == null, "Returning to the old area resurrected the dismissed dog");
                checkPip(companion);
                next(Step.SETTLE);
            }
            case SETTLE -> {
                if (companion == null || companion.distanceToSqr(owner) >= 36) break;
                if (ticks < 85) break;
                // Let the automatic reunion finish before positioning either subject.
                // Keep the owner behind the camera and within the normal greeting distance.
                place(owner, YARD.north(2));
                companion.sitDown();
                companion.setNoAi(true);
                companion.snapTo(YARD, -145, 0);
                visitor.setGameMode(GameType.SPECTATOR);
                frame(visitor, companion);
                cue(visitor, LOOK);
                next(Step.PREPARE_PHOTO);
            }
            case PREPARE_PHOTO -> {
                frame(visitor, companion);
                if (ticks < 25) break;
                level.explode(null, companion.getX() - 1.4, companion.getY(), companion.getZ(), 1.5F,
                        net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                next(Step.PHOTO);
            }
            case PHOTO -> {
                if (companion == null) break;
                frame(visitor, companion);
                if (!Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-blast-duck.png"))
                        || !Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-after-blast.png"))) {
                    if (ticks % 100 == 0) level.explode(null, companion.getX() - 1.4, companion.getY(), companion.getZ(),
                            1.5F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                    break;
                }
                next(Step.TUMBLE);
            }
            case TUMBLE -> {
                if (companion == null) break;
                frame(visitor, companion);
                if (!Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-tumble.png"))) {
                    if (ticks % 80 == 0) companion.causeFallDamage(14, 1f, companion.damageSources().fall());
                    break;
                }
                next(Step.LINE_UP);
            }
            case LINE_UP -> {
                if (ticks < 75) break;
                // Lower the attacker's feet so a vanilla survival swing crosses the short dog's torso.
                var footprint = YARD.north(1).below();
                level.setBlockAndUpdate(footprint.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(footprint, Blocks.AIR.defaultBlockState());
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                owner.teleportTo(level, YARD.getX() + 0.5, YARD.getY() - 1, YARD.getZ() - 0.65, Set.of(), 0, 0, true);
                companion.snapTo(YARD, -145, 0);
                target = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
                require(target != null, "Could not create the target for the melee swing");
                target.setNoAi(true);
                target.snapTo(YARD.getX() + 0.5, YARD.getY(), YARD.getZ() + 1.8, 0, 0);
                require(level.addFreshEntity(target), "Could not spawn the melee target behind Pip");
                cue(owner, SWING);
                next(Step.THROUGH_DOG);
            }
            case THROUGH_DOG -> {
                if (target.getHealth() == target.getMaxHealth()) break;
                require(companion.getHealth() == companion.getMaxHealth() && companion.dangerReaction().name().equals("NONE"),
                        "Companion absorbed part of the melee swing aimed through it");
                target.discard();
                cue(owner, SWING);
                next(Step.ACCIDENT);
            }
            case ACCIDENT -> {
                if (!Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-accidental-hit.png"))) break;
                require(companion.isAlive() && companion.getHealth() == companion.getMaxHealth(), "Direct swing injured Pip");
                companion.setNoAi(false);
                companion.standUp();
                command(owner, "frenchie select " + COURIER);
                next(Step.SAVE);
            }
            case SAVE -> {
                require(courier == null && companion != null && companion.isAlive(), "Incorrect state at shutdown");
                require(level.getServer().saveEverything(false, true, true), "Failed to save active and dismissed Frenchies");
                return true;
            }
            case REJOIN -> {
                if (companion == null || companion.distanceToSqr(owner) >= 36) break;
                require(courier == null && !companion.isOrderedToSit(), "Reconnect changed active or dismissed state");
                checkPip(companion);
                owner.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), INN, 0, 0), true), false);
                owner.kill(level);
                next(Step.RESPAWN);
            }
            case RESPAWN -> {
                require(courier == null, "Player death recalled a deliberately dismissed courier");
                if (!owner.isAlive() || owner.position().distanceToSqr(INN.getCenter()) > 36
                        || companion == null || companion.distanceToSqr(owner) >= 36) break;
                checkPip(companion);
                require(owner.getLastDeathLocation().isPresent(), "Respawn did not record the old location");
                command(owner, "frenchie whistle");
                var inventory = owner.getInventory();
                for (int i = 0; i < 9; i++) if (inventory.getItem(i).is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) inventory.setSelectedSlot(i);
                require(owner.getMainHandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get()), "Could not replace the lost whistle");
                cue(owner, CALL_COURIER);
                next(Step.LAST_CALL);
            }
            case LAST_CALL -> {
                if (courier == null || courier.distanceToSqr(owner) >= 36 || companion == null) break;
                require(courier.isOwnedBy(owner) && courier.getName().getString().equals("Violet")
                                && !courier.isDelivering() && ItemStack.matches(courier.getParcel(), CourierParcel.parcel())
                                && ItemStack.matches(courier.getItemBySlot(EquipmentSlot.FEET), named(new ItemStack(Items.LEATHER_BOOTS), "Violet's boots"))
                                && visitor.getInventory().countItem(Items.BUNDLE) == 0 && owner.getInventory().countItem(Items.BUNDLE) == 0,
                        "Server restart, dismissal or respawn lost cargo identity or sent the parcel");
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                owner.snapTo(courier.position().add(0.9, 0, 0), 0, 0);
                owner.interactOn(courier, InteractionHand.MAIN_HAND, Vec3.ZERO);
                require(ItemStack.matches(owner.getMainHandItem(), CourierParcel.parcel()) && courier.getParcel().isEmpty()
                                && level.getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(owner)).size() == 2,
                        "Could not collect precisely one copy after reconnect and respawn");
                return true;
            }
        }
        return false;
    }

    // A separate disposable world. Removing its locator file between phases is safe because
    // all dog and item data live exclusively in this one unloaded entity chunk.
    static boolean legacy(ServerPlayer owner, ServerPlayer visitor, int phase) {
        var level = owner.level();
        var dog = (FrenchieEntity) level.getEntity(COMPANION);
        if (++ticks > 2000) throw new AssertionError("Legacy save fixture timed out at " + step);
        switch (step) {
            case START -> {
                if (phase == 2) {
                    require(!level.hasChunkAt(YARD) && dog == null, "The owner revisited the kennel before migration");
                    next(Step.REJOIN);
                    break;
                }
                platform(owner, YARD);
                platform(owner, INN);
                owner.setGameMode(GameType.SURVIVAL);
                visitor.setGameMode(GameType.SURVIVAL);
                place(owner, YARD);
                place(visitor, YARD.east(3));
                dog = spawn(owner, COMPANION, "Pip", YARD.west(2));
                dog.setItemSlot(EquipmentSlot.OFFHAND, named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Pip's squeaker"));
                next(Step.SAVE);
            }
            case SAVE -> {
                if (ticks < 20) break;
                require(dog != null && dog.isOrderedToSit() && dog.isAlive()
                                && level.getEntities(FrenchieRegistry.FRENCHIE.get(), d -> true).size() == 1,
                        "Upgrade fixture requires exactly one live dog and no dismissed dog");
                checkPip(dog);
                place(owner, INN);
                place(visitor, INN.east(3));
                next(Step.LEAVE);
            }
            case LEAVE -> {
                if (level.hasChunkAt(YARD) || dog != null) break;
                require(level.getServer().saveEverything(false, true, true), "Could not save the old kennel");
                return true;
            }
            case REJOIN -> {
                if (ticks == 1 || ticks % 100 == 0) command(owner, "frenchie whistle");
                if (!owner.getInventory().contains(stack -> stack.is(FrenchieRegistry.FRENCHIE_WHISTLE.get()))) break;
                require(!level.hasChunkAt(YARD) && dog == null, "Migration force-loaded the old kennel to enumerate dogs");
                var inventory = owner.getInventory();
                for (int i = 0; i < 9; i++) if (inventory.getItem(i).is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) inventory.setSelectedSlot(i);
                cue(owner, CALL_ACTIVE);
                next(Step.CALL);
            }
            case CALL -> {
                if (dog == null || dog.distanceToSqr(owner) >= 36) break;
                require(dog.isOwnedBy(owner) && !dog.isOrderedToSit(), "Migration changed ownership or refused the recall");
                checkPip(dog);
                place(visitor, YARD);
                next(Step.RETURN_TO_KENNEL);
            }
            case RETURN_TO_KENNEL -> {
                if (!level.areEntitiesActuallyLoadedAndTicking(net.minecraft.world.level.ChunkPos.containing(YARD))) break;
                require(dog != null && dog.distanceToSqr(owner) < 36
                                && level.getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(owner)).size() == 1,
                        "Revisiting legacy terrain duplicated the dog or its toy");
                return true;
            }
        }
        return false;
    }

    private static void frame(ServerPlayer visitor, FrenchieEntity dog) {
        var center = YARD.getBottomCenter();
        if (dog.position().distanceToSqr(center) > 0.01) dog.snapTo(center, -145, 0);
        dog.setDeltaMovement(Vec3.ZERO);
        dog.setYRot(-145);
        dog.setYHeadRot(-145);
        dog.setYBodyRot(-145);
        var camera = center.add(1.15, -1.03, -1.35);
        if (visitor.position().distanceToSqr(camera) > 0.02) {
            visitor.teleportTo(visitor.level(), camera.x, camera.y, camera.z, Set.of(), visitor.getYRot(), visitor.getXRot(), true);
        }
    }

    private static void checkPip(FrenchieEntity dog) {
        require(dog.getName().getString().equals("Pip") && ItemStack.matches(dog.getOffhandItem(),
                named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Pip's squeaker")), "Pip lost his name or toy");
    }

    private static void next(Step value) {
        step = value;
        ticks = 0;
        LogUtils.getLogger().info("Whistle fixture reached {}", step);
    }

    private static void platform(ServerPlayer owner, BlockPos center) {
        for (var pos : BlockPos.betweenClosed(center.offset(-7, -1, -7), center.offset(7, -1, 7))) {
            owner.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    private static FrenchieEntity spawn(ServerPlayer owner, UUID id, String name, BlockPos pos) {
        var dog = FrenchieRegistry.FRENCHIE.get().create(owner.level(), EntitySpawnReason.COMMAND);
        require(dog != null, "Could not create " + name);
        dog.setUUID(id);
        dog.setCustomName(Component.literal(name));
        dog.tame(owner);
        dog.sitDown();
        dog.snapTo(pos, 0, 0);
        require(owner.level().addFreshEntity(dog), "Could not spawn " + name);
        return dog;
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void command(ServerPlayer owner, String text) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(), text);
    }

    private static void place(ServerPlayer player, BlockPos feet) {
        player.teleportTo(player.level(), feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, Set.of(), 0, 0, true);
    }

    private static void cue(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
