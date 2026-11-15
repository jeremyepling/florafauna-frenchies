package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class FrontpackAccessorySlots {
    private FrontpackAccessorySlots() {}

    public static ItemStack findEquipped(LivingEntity entity) {
        if (ModList.get().isLoaded("curios")) {
            var pack = CuriosSlots.find(entity);
            if (!pack.isEmpty()) return pack;
        }
        return ModList.get().isLoaded("trinkets_updated") ? TrinketSlots.find(entity) : ItemStack.EMPTY;
    }

    private static final class CuriosSlots {
        private static ItemStack find(LivingEntity entity) {
            var slots = CuriosApi.getCuriosInventoryOrNull(entity);
            if (slots == null) return ItemStack.EMPTY;
            // The predicate overload sees equips and removals immediately, including in the same tick.
            return slots.findFirstCurio(stack -> stack.is(FrontpackEquipment.PACKS))
                    .map(SlotResult::stack).orElse(ItemStack.EMPTY);
        }
    }

    private static final class TrinketSlots {
        private static ItemStack find(LivingEntity entity) {
            var slots = TrinketsApi.getAttachment(entity);
            if (slots == null) return ItemStack.EMPTY;
            // Limit matches to slots that can apply effects, so cosmetic packs do not count as equipment.
            return slots.findFirst(FrontpackEquipment.PACKS, true)
                    .map(TrinketSlotAccess::get).orElse(ItemStack.EMPTY);
        }
    }
}
