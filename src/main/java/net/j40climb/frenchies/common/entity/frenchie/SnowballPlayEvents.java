package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** A landing snowball is only an invitation to investigate nearby safe snow. */
@EventBusSubscriber(modid = Frenchies.MOD_ID)
public final class SnowballPlayEvents {
    private SnowballPlayEvents() {}

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().getType() != EntityTypes.SNOWBALL
                || !(event.getProjectile().level() instanceof ServerLevel level)) return;
        var impact = event.getRayTraceResult().getLocation();
        for (FrenchieEntity dog : level.getEntitiesOfClass(FrenchieEntity.class,
                new AABB(impact, impact).inflate(6), FrenchieEntity::isTame)) {
            dog.inviteSnowball(impact);
        }
    }
}
