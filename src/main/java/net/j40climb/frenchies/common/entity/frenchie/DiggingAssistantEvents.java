package net.j40climb.frenchies.common.entity.frenchie;

import java.util.Comparator;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ShovelItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Frenchies.MOD_ID)
public final class DiggingAssistantEvents {
    private DiggingAssistantEvents() {}

    @SubscribeEvent
    public static void ownerDigs(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer().getMainHandItem().getItem() instanceof ShovelItem)
                || !DiggingAssistantGoal.diggable(event.getState())
                || level.getRandom().nextInt(5) != 0) return;
        level.getEntitiesOfClass(FrenchieEntity.class, event.getPlayer().getBoundingBox().inflate(7),
                        dog -> dog.isOwnedBy(event.getPlayer()) && dog.distanceToSqr(event.getPlayer()) < 49)
                .stream().sorted(Comparator.comparingDouble(dog -> dog.distanceToSqr(event.getPlayer())))
                .anyMatch(dog -> dog.requestDigging(event.getPos()));
    }
}
