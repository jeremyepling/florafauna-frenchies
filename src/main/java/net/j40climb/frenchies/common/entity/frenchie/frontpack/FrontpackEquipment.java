package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

final class FrontpackEquipment {
    static final TagKey<Item> PACKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "front_packs"));

    static ItemStack wornBy(LivingEntity wearer) {
        var chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(PACKS)) return chest;
        var accessory = FrontpackAccessorySlots.findEquipped(wearer);
        if (!accessory.isEmpty()) return accessory;
        //? if <26.3 {
        if (wearer instanceof Avatar avatar && ModList.get().isLoaded("baubles")) {
            return BaublesFrontpack.findEquipped(avatar);
        }
        //?}
        return ItemStack.EMPTY;
    }
}
