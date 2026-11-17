package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep the normal interaction hitbox. A melee swing can reach another target behind the companion. */
@Mixin(Minecraft.class)
abstract class CompanionAttackMixin {
    @ModifyArg(method = "startAttack", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"))
    private Entity frenchies$attackThroughCompanion(Entity target) {
        if (!(target instanceof FrenchieEntity dog) || !dog.isTame()) return target;
        var player = ((Minecraft) (Object) this).player;
        var reach = player.getAttackRangeWith(player.getMainHandItem());
        var hit = reach.getClosesetHit(player, 1F, entity -> entity.isPickable() && !entity.isSpectator()
                && !(entity instanceof FrenchieEntity other && other.isTame()));
        if (hit instanceof EntityHitResult behind && reach.isInRange(player, behind.getLocation())) return behind.getEntity();
        return target;
    }
}
