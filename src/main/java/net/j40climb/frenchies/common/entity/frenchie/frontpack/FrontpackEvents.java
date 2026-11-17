package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.CourierTrips;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler for Frenchie frontpack pickup/putdown interactions.
 *
 * Pickup: Shift-right-click on tamed Frenchie (owned by you)
 * Put down: Shift-right-click air, or remove the last equipped front pack.
 */
@EventBusSubscriber(modid = Frenchies.MOD_ID)
public class FrontpackEvents {

    /**
     * Handle shift-right-click on Frenchie to pick it up.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        // Pickup conditions
        if (!player.isShiftKeyDown()) return;
        if (!(target instanceof FrenchieEntity frenchie)) return;
        if (!frenchie.isTame()) return;
        if (!frenchie.isOwnedBy(player)) return;
        var heldItem = player.getItemInHand(event.getHand());
        if (frenchie.isBaby() || frenchie.isPassenger() || heldItem.is(FrenchieRegistry.SQUEAKY_TOY.get())
                || heldItem.is(FrenchieRegistry.BUBBLE_TOY.get())) return;
        if (FrontpackEquipment.wornBy(player).isEmpty()) {
            var held = player.getItemInHand(event.getHand());
            if (held.isEmpty() || held.is(FrontpackEquipment.PACKS)) {
                // Keep a failed pickup from toggling Sit or canceling the dog's current job.
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (!player.level().isClientSide()) {
                    player.sendOverlayMessage(Component.translatable("frenchie.florafauna.frontpack.equip_first"));
                }
            }
            return;
        }

        // Check if already carrying a Frenchie
        FrontpackData currentData = player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
        if (currentData.hasCarriedFrenchie()) return;

        // Client: just cancel event and return success
        if (player.level().isClientSide()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // Server: serialize Frenchie to NBT and store in attachment
        TagValueOutput output = TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING,
            player.level().registryAccess()
        );
        frenchie.saveWithoutId(output);
        CompoundTag frenchieNBT = output.buildResult();

        FrontpackData newData = new FrontpackData(
            true,
            frenchieNBT,
            player.level().getGameTime()
        );

        player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, newData);
        if (frenchie.hasCompanionJob()) player.sendOverlayMessage(Component.translatable("frenchie.florafauna.order.packed"));

        // Despawn the entity
        CourierTrips.forget(frenchie);
        frenchie.discard();

        // Play pickup sound
        player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void releaseWhenUnequipped(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        var data = player.getExistingDataOrNull(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
        if (data == null || !data.hasCarriedFrenchie()) return;
        if (!FrontpackEquipment.wornBy(player).isEmpty()) {
            if (data.releasePending()) player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, data.withReleasePending(false));
            return;
        }
        if (data.releasePending() && player.tickCount % 10 != 0) return;
        if (!releaseNearby(player, data) && !data.releasePending()) {
            player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, data.withReleasePending(true));
            player.sendOverlayMessage(Component.translatable("frenchie.florafauna.frontpack.wait_for_ground"));
        }
    }

    private static boolean releaseNearby(ServerPlayer player, FrontpackData data) {
        var position = CourierTrips.findLanding(player.level(), player.blockPosition(), player);
        return position != null && releaseAt(player, player.level(), data, position);
    }

    public static void putDown(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        FrontpackData data = player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
        if (!data.hasCarriedFrenchie()) return;

        var type = FrenchieRegistry.FRENCHIE.get();
        // Use yaw for the ground target so looking down never puts the dog inside the floor.
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        BlockPos target = BlockPos.containing(player.position().add(forward.scale(2)));
        if (!level.isInWorldBounds(target)) return;
        Vec3 position = DismountHelper.findSafeDismountLocation(type, level, target, true);
        if (position != null) releaseAt(player, level, data, position);
    }

    private static boolean releaseAt(Player player, ServerLevel level, FrontpackData data, Vec3 position) {
        var dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (dog == null) return false;
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.frenchieNBT()));
        dog.snapTo(position, player.getYRot(), 0);
        if (!level.noCollision(dog, dog.getBoundingBox()) || !level.addFreshEntity(dog)) return false;

        player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, FrontpackData.DEFAULT);
        if (dog.hasCompanionJob()) player.sendOverlayMessage(Component.translatable("frenchie.florafauna.order.resumed"));
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.0f);
        return true;
    }
}
