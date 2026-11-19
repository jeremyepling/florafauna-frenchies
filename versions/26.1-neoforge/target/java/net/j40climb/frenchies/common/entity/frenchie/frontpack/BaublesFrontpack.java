package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import tld.unknown.baubles.api.BaubleType;
import tld.unknown.baubles.api.Baubles;

final class BaublesFrontpack {
    static ItemStack findEquipped(Avatar wearer) {
        var stack = Baubles.API.getBaublesInventory(wearer).getBaubleInSlot(BaubleType.BODY);
        return stack.is(FrontpackEquipment.PACKS) ? stack : ItemStack.EMPTY;
    }
}
