package net.j40climb.frenchies.common.entity.frenchie;

import java.util.Comparator;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//? if <26.3 {
import net.neoforged.neoforge.event.level.BlockEvent;
//?} else {
/*import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
*///?}

@EventBusSubscriber(modid = Frenchies.MOD_ID)
public final class DiggingAssistantEvents {
    private DiggingAssistantEvents() {}

    @SubscribeEvent
    //? if <26.3 {
    public static void ownerDigs(BlockEvent.BreakEvent event) {
    //?} else {
    /*public static void ownerDigs(BreakBlockEvent event) {
    *///?}
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !event.getPlayer().getMainHandItem().is(ItemTags.SHOVELS)
                || !DiggingAssistantGoal.diggable(event.getState())
                || level.getRandom().nextInt(5) != 0) return;
        level.getEntitiesOfClass(FrenchieEntity.class, event.getPlayer().getBoundingBox().inflate(7),
                        dog -> dog.isOwnedBy(event.getPlayer()) && dog.distanceToSqr(event.getPlayer()) < 49)
                .stream().sorted(Comparator.comparingDouble(dog -> dog.distanceToSqr(event.getPlayer())))
                .anyMatch(dog -> dog.requestDigging(event.getPos()));
    }
}
