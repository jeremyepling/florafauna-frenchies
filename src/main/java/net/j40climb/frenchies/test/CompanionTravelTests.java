package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.UUID;

final class CompanionTravelTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    private static final int BOW = 1, RUN = 2, HOP = 3, SPLOOT = 4;

    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        var player = h.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.snapTo(h.absoluteVec(new Vec3(x, 1, z)), 0, 0);
        return player;
    }

    private static void disconnect(ServerPlayer... players) {
        for (ServerPlayer player : players) {
            player.level().getServer().getPlayerList().remove(player);
        }
    }

    private static FrenchieEntity dog(GameTestHelper h, ServerPlayer owner) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        dog.standUp();
        return dog;
    }

    private static FrenchieEntity current(GameTestHelper h, UUID id) {
        var entity = h.getLevel().getEntity(id);
        h.assertTrue(entity instanceof FrenchieEntity, "Courier is still travelling");
        return (FrenchieEntity) entity;
    }

    private static FrenchieEntity reload(GameTestHelper h, FrenchieEntity original) {
        var level = h.getLevel();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        original.saveWithoutId(output);
        var saved = output.buildResult();
        var restored = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
        h.assertTrue(restored != null, "Could not reconstruct Frenchie");
        original.discard();
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        h.assertTrue(level.addFreshEntity(restored) && restored.getUUID().equals(original.getUUID()),
                "Entity reload did not restore the same Frenchie");
        return restored;
    }

    static void interruptCelebration(GameTestHelper h) {
        var owner = player(h, 5.5, 4);
        var dog = dog(h, owner);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed"))
                .thenExecute(dog::tryCelebrate)
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() != 0, "Celebration never began"))
                .thenExecute(() -> owner.interactOn(dog, HAND, Vec3.ZERO))
                .thenWaitUntil(() -> h.assertTrue(dog.isOrderedToSit() && dog.celebrationStage() == 0,
                        "Sit order failed to interrupt celebration"))
                .thenExecute(() -> {
                    var restored = reload(h, dog);
                    h.assertTrue(restored.isOrderedToSit() && restored.celebrationStage() == 0,
                            "A reload discarded the sit order or resumed an interrupted celebration");
                    disconnect(owner);
                }).thenSucceed();
    }

    static void fetch(GameTestHelper h) {
        var owner = player(h, 5.5, 3.5);
        var visitor = player(h, 6.5, 3.5);
        var dog = dog(h, owner);
        var id = dog.getUUID();
        var toy = new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get());
        toy.set(DataComponents.CUSTOM_NAME, Component.literal("Pocket squeaker"));
        owner.setItemInHand(HAND, toy.copy());
        owner.setXRot(70);
        owner.setShiftKeyDown(true);
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        owner.setShiftKeyDown(false);
        h.assertTrue(owner.getMainHandItem().isEmpty(), "Throw did not release the held toy");
        var loose = h.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(6),
                item -> ItemStack.matches(item.getItem(), toy));
        h.assertTrue(loose.size() == 1, "Throw should create exactly one named toy");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(dog.getOffhandItem(), toy), "Dog never picked up the toy"))
                .thenExecute(() -> {
                    h.assertTrue(loose.getFirst().isRemoved(), "Dog and ground both retained the thrown toy");
                    dog.sitDown();
                    var restored = reload(h, dog);
                    h.assertTrue(restored.isOrderedToSit() && ItemStack.matches(restored.getOffhandItem(), toy),
                            "Carried toy or sit order lost during reload");
                    visitor.snapTo(restored.position().add(1, 0, 0), 0, 0);
                    visitor.interactOn(restored, HAND, Vec3.ZERO);
                    h.assertTrue(visitor.getMainHandItem().isEmpty() && ItemStack.matches(restored.getOffhandItem(), toy),
                            "Visitor took the owner's toy");
                    owner.snapTo(restored.position().add(-1, 0, 0), 0, 0);
                    owner.interactOn(restored, HAND, Vec3.ZERO);
                    h.assertTrue(ItemStack.matches(owner.getMainHandItem(), toy) && current(h, id).getOffhandItem().isEmpty(),
                            "The owner should recover exactly one named toy from the seated dog");
                    disconnect(visitor, owner);
                }).thenSucceed();
    }

    static void captain(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var guest = player(h, 6.5, 4.5);
        var dog = dog(h, owner);
        var boat = h.spawn(EntityType.OAK_BOAT, 5, 1, 6);
        h.assertTrue(!dog.hasFlotationVest(), "Frenchie showed its flotation vest on land");
        owner.interactOn(boat, HAND, Vec3.ZERO);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(owner.getVehicle() == boat && dog.getVehicle() == boat,
                        "Following Frenchie did not automatically board the owner's boat"))
                .thenExecute(() -> {
                    h.assertTrue(!dog.hasFlotationVest(), "Captain showed a vest in a dry boat");
                    guest.interactOn(boat, HAND, Vec3.ZERO);
                    h.assertTrue(guest.getVehicle() == boat && boat.getControllingPassenger() == owner,
                            "Frenchie took the second player seat or displaced the driver");
                    // Three water blocks across, with a solid dock near either passenger.
                    for (var pos : BlockPos.betweenClosed(new BlockPos(3, 1, 4), new BlockPos(7, 1, 8))) {
                        boolean dock = pos.getX() == 3 || pos.getX() == 7 || pos.getZ() == 4 || pos.getZ() == 8;
                        h.setBlock(pos, dock ? Blocks.STONE : Blocks.WATER);
                    }
                })
                .thenWaitUntil(() -> h.assertTrue(dog.getVehicle() == boat && dog.hasFlotationVest(),
                        "Floating captain never showed its vest"))
                .thenExecute(owner::stopRiding)
                .thenWaitUntil(() -> h.assertTrue(!dog.isPassenger()
                                && h.getLevel().getBlockState(dog.blockPosition().below()).is(Blocks.STONE),
                        "Captain failed to disembark onto the safe dock when its owner left"))
                .thenExecute(() -> {
                    h.assertTrue(guest.getVehicle() == boat && !dog.hasFlotationVest(),
                            "Owner exit ejected the second player or left the dog wearing the vest");
                    h.assertTrue(!reload(h, dog).hasFlotationVest(), "Reload restored a vest on dry ground");
                    disconnect(guest, owner);
                }).thenSucceed();
    }

    static void audience(GameTestHelper h) {
        var owner = player(h, 5.5, 5.5);
        var listeners = new ArrayList<FrenchieEntity>();
        for (int x = 3; x <= 7; x++) {
            var listener = h.spawn(FrenchieRegistry.FRENCHIE.get(), x, 1, 6);
            listener.tame(owner);
            listener.standUp();
            listeners.add(listener);
        }
        var puppy = listeners.get(2);
        puppy.setAge(-24000);
        var seated = listeners.getLast();
        seated.sitDown();
        owner.setItemInHand(HAND, new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()));
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        h.assertTrue(puppy.toyEarLift(0) > 0 && seated.toyEarLift(0) == 0
                        && listeners.stream().noneMatch(dog -> dog.toyPlay().watching(owner)),
                "First squeak should perk the puppy's ears and respect the sitting order");
        // GameTest's EmbeddedChannel is outside the server network tick loop. In a real
        // connection ServerGamePacketListenerImpl calls player.doTick(), advancing item cooldowns.
        h.startSequence().thenExecuteFor(20, owner.getCooldowns()::tick).thenExecute(() -> {
            h.assertTrue(!owner.getCooldowns().isOnCooldown(owner.getMainHandItem()), "Second squeak is still on cooldown");
            owner.getMainHandItem().use(owner.level(), owner, HAND);
            long count = listeners.stream().filter(dog -> dog.toyPlay().watching(owner)).count();
            h.assertTrue(count == 3 && !seated.toyPlay().watching(owner),
                    "Expected three available audience members, found " + count);
        }).thenExecuteAfter(80, () -> {
            SqueakyToyItem.squeak(h.getLevel(), puppy);
        }).thenExecuteAfter(18, () -> {
            SqueakyToyItem.squeak(h.getLevel(), puppy);
            h.assertTrue(listeners.stream().noneMatch(dog -> dog.toyPlay().watching(puppy)),
                    "A dog's own squeaks recruited another audience");
            disconnect(owner);
        }).thenSucceed();
    }

    static void zoomies(GameTestHelper h, boolean open) {
        var owner = player(h, 9.5, 9.5);
        var dog = dog(h, owner);
        var sequence = h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie has not landed"))
                .thenExecute(dog::tryCelebrate)
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == BOW, "No opening play bow"));
        if (open) {
            sequence.thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN, "No run on the clear stone yard"));
            double centerZ = dog.getZ();
            for (int lap = 0; lap < 2; lap++) {
                sequence.thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN && dog.getZ() > centerZ + 1,
                                "Dog did not reach the south side of lap"))
                        .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN && dog.getZ() < centerZ - 1,
                                "Dog did not reach the north side of lap"));
            }
        } else {
            sequence.thenExecute(() -> {
                var crop = dog.blockPosition().west(2).south(2);
                h.getLevel().setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
                h.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            });
        }
        boolean[] ran = {false};
        sequence.thenWaitUntil(() -> {
            ran[0] |= dog.celebrationStage() == RUN;
            h.assertTrue(dog.celebrationStage() == HOP, "No finish hop");
        }).thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == SPLOOT, "No belly-down landing"))
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == 0, "Sploot became permanent"))
                .thenExecute(() -> {
                    h.assertTrue(open || !ran[0], "Dog ran a lap through crops");
                    h.assertTrue(!dog.isOrderedToSit() && !dog.isFrenchieSleeping(), "Celebration imposed a rest order");
                    dog.tryCelebrate();
                }).thenExecuteAfter(8, () -> {
                    h.assertTrue(dog.celebrationStage() == 0, "Celebration cooldown allowed an immediate restart");
                    disconnect(owner);
                }).thenSucceed();
    }
}
