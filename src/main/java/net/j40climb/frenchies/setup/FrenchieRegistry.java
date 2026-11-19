package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.WellRestedEffect;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackElytraRecipe;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackWarning;
import net.j40climb.frenchies.common.item.FrenchieWhistleItem;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.j40climb.frenchies.common.item.BubbleToyItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public final class FrenchieRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Frenchies.LEGACY_NAMESPACE);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Frenchies.LEGACY_NAMESPACE);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Frenchies.LEGACY_NAMESPACE);

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Frenchies.LEGACY_NAMESPACE);

    public static final DeferredRegister<TicketType> TICKET_TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, Frenchies.LEGACY_NAMESPACE);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Frenchies.LEGACY_NAMESPACE);

    public static final ResourceKey<EntityType<?>> FRENCHIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie"));

    public static final Supplier<EntityType<FrenchieEntity>> FRENCHIE = ENTITY_TYPES.register("frenchie",
            () -> EntityType.Builder.of(FrenchieEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f).build(FRENCHIE_KEY));

    public static final DeferredItem<Item> FRENCHIE_SPAWN_EGG = ITEMS.registerItem("frenchie_spawn_egg", properties ->
            new SpawnEggItem(properties.spawnEgg(FRENCHIE.get())));

    public static final DeferredItem<FrenchieWhistleItem> FRENCHIE_WHISTLE =
            ITEMS.registerItem("frenchie_whistle", FrenchieWhistleItem::new,
                    properties -> properties.stacksTo(1).component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.florafauna.frenchie_whistle.use"),
                            Component.translatable("item.florafauna.frenchie_whistle.select")))));
    public static final DeferredItem<SqueakyToyItem> SQUEAKY_TOY = ITEMS.registerItem("squeaky_toy", SqueakyToyItem::new);
    public static final DeferredItem<BubbleToyItem> BUBBLE_TOY = ITEMS.registerItem("bubble_toy", BubbleToyItem::new);

    public static final Supplier<EntityType<PlayBubbleEntity>> PLAY_BUBBLE = ENTITY_TYPES.register("play_bubble",
            () -> EntityType.Builder.of(PlayBubbleEntity::new, MobCategory.MISC)
                    .sized(0.20F, 0.20F).noSave().noSummon().noLootTable().clientTrackingRange(4).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "play_bubble"))));

    private static final ItemLore FRONT_PACK_LORE = new ItemLore(List.of(
            Component.translatable("item.florafauna.front_pack.equip"),
            Component.translatable("item.florafauna.front_pack.dye")));

    public static final DeferredItem<Item> FRONT_PACK = ITEMS.registerSimpleItem("front_pack",
            properties -> properties.stacksTo(1).equippable(EquipmentSlot.CHEST).component(DataComponents.LORE, FRONT_PACK_LORE));

    public static final DeferredItem<Item> WINGED_FRONT_PACK = ITEMS.registerSimpleItem("winged_front_pack",
            properties -> properties.durability(432).rarity(Rarity.EPIC).repairable(Items.PHANTOM_MEMBRANE)
                    .component(DataComponents.GLIDER, Unit.INSTANCE)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(EquipmentAssets.ELYTRA)
                            .setDamageOnHurt(false).build())
                    .component(DataComponents.LORE, FRONT_PACK_LORE));

    public static final Supplier<RecipeSerializer<FrontpackElytraRecipe>> FRONT_PACK_ELYTRA_RECIPE =
            RECIPE_SERIALIZERS.register("front_pack_elytra", () -> FrontpackElytraRecipe.SERIALIZER);

    public static final Supplier<TicketType> COURIER_STOP = TICKET_TYPES.register("courier_stop",
            () -> new TicketType(120, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_PERSIST));

    public static final Holder<MobEffect> WELL_RESTED = MOB_EFFECTS.register("well_rested", WellRestedEffect::new);

    public static final Supplier<AttachmentType<FrontpackWarning>> FRENCHIE_WARNING =
            ATTACHMENT_TYPES.register("frenchie_warning", () -> AttachmentType.builder(() -> FrontpackWarning.NONE)
                    .sync(FrontpackWarning.STREAM_CODEC).build());

    public static final Supplier<AttachmentType<FrontpackData>> FRENCH_FRONTPACK_DATA =
            ATTACHMENT_TYPES.register("french_frontpack_data", () ->
                    AttachmentType.builder(() -> FrontpackData.DEFAULT)
                            .serialize(FrontpackData.CODEC.fieldOf("french_frontpack_data"))
                            .sync(FrontpackData.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        SqueakyToyItem.SOUNDS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        ENTITY_TYPES.register(bus);
        MOB_EFFECTS.register(bus);
        TICKET_TYPES.register(bus);
        ATTACHMENT_TYPES.register(bus);
    }
}
