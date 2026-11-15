package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

enum FrontpackTint implements ItemTintSource {
    INSTANCE;

    static final MapCodec<FrontpackTint> CODEC = MapCodec.unit(INSTANCE);

    static int fabricColor(int dye) {
        int color = ARGB.opaque(dye);
        return ARGB.srgbLerp(0.25F, color, ARGB.greyscale(color));
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        var dye = stack.get(DataComponents.DYED_COLOR);
        return dye == null ? 0xFF393939 : fabricColor(dye.rgb());
    }

    @Override
    public MapCodec<FrontpackTint> type() {
        return CODEC;
    }
}
