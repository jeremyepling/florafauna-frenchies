package net.j40climb.frenchies.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractBoat.class)
abstract class BoatCaptainMixin {
    @Shadow protected abstract double rideHeight(EntityDimensions dimensions);

    // Keep vanilla riding, saving and passenger packets. Only the boat's seat accounting omits the bow rider.
    @ModifyExpressionValue(method = {"canAddPassenger", "getPassengerAttachmentPoint", "positionRider", "tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;getPassengers()Ljava/util/List;"))
    private List<Entity> frenchies$seatedPassengers(List<Entity> passengers) {
        for (Entity passenger : passengers) {
            if (passenger instanceof FrenchieEntity dog && dog.isTame()) {
                return passengers.stream().filter(p -> !(p instanceof FrenchieEntity pet && pet.isTame())).toList();
            }
        }
        return passengers;
    }

    // If a driver leaves before the bow rider can dismount, the remaining seated player still steers.
    @ModifyExpressionValue(method = {"tick", "getControllingPassenger"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;getFirstPassenger()Lnet/minecraft/world/entity/Entity;"))
    private Entity frenchies$driver(Entity first) {
        if (first instanceof FrenchieEntity dog && dog.isTame()) {
            return frenchies$seatedPassengers(((AbstractBoat) (Object) this).getPassengers()).stream().findFirst().orElse(null);
        }
        return first;
    }

    // The dog can join even when all ordinary seats are occupied. Its own canRide checks the owner and bow.
    @ModifyExpressionValue(method = "canAddPassenger", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int frenchies$occupiedSeats(int occupied, Entity passenger) {
        return passenger instanceof FrenchieEntity dog && dog.isTame() ? 0 : occupied;
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("HEAD"), cancellable = true)
    private void frenchies$bowPosition(Entity passenger, EntityDimensions dimensions, float partialTick,
                                        CallbackInfoReturnable<Vec3> result) {
        if (passenger instanceof FrenchieEntity dog && dog.isTame()) {
            AbstractBoat boat = (AbstractBoat) (Object) this;
            result.setReturnValue(new Vec3(0, rideHeight(dimensions) + 0.14, 0.68)
                    .yRot(-boat.getYRot() * Mth.DEG_TO_RAD));
        }
    }
}
