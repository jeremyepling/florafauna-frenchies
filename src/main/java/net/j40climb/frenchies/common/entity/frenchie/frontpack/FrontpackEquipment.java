package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

final class FrontpackEquipment {
    static ItemStack wornBy(LivingEntity wearer) {
        var chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(FrenchieRegistry.FRONT_PACK.get())) return chest;
        var accessory = FrontpackAccessorySlots.findEquipped(wearer);
        if (!accessory.isEmpty()) return accessory;
        if (wearer instanceof Avatar avatar && ModList.get().isLoaded("baubles")) {
            return BaublesFrontpack.findEquipped(avatar);
        }
        return ItemStack.EMPTY;
    }
}
