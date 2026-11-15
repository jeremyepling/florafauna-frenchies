package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Client input and rendering for the carried Frenchie.
 */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public class FrontpackClientEvents {
    static final ContextKey<Float> ALERT_YAW = new ContextKey<>(Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frontpack_alert_yaw"));
    private static final Identifier CREEPER_SPRITE = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie/creeper");

    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "front_pack"), FrontpackTint.CODEC);
    }

    @SubscribeEvent
    public static void registerWarningHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frontpack_warning"), (gui, delta) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;
            var warning = mc.player.getExistingDataOrNull(FrenchieRegistry.FRENCHIE_WARNING);
            long now = mc.player.level().getGameTime();
            if (warning == null || !warning.visible(now)) return;
            var pack = mc.player.getExistingDataOrNull(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
            if (pack == null || !pack.hasCarriedFrenchie()) return;
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, CREEPER_SPRITE, gui.guiWidth() / 2 - 8, gui.guiHeight() - 51, 16, 16);
        });
    }

    /**
     * Registers the frontpack model layer definition.
     * This bakes the 3D geometry from FrenchFrontpackModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                FrontpackModel.LAYER_LOCATION,
                FrontpackModel::createBodyLayer
        );
    }

    /**
     * Adds the frontpack render layer to all player renderers.
     * This includes both "default" and "slim" player models.
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        // Iterate through all player skin types (default, slim)
        for (PlayerModelType skinType : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skinType);
            if (renderer != null) {
                // Add the Frenchie frontpack layer to this player renderer
                renderer.addLayer(new FrontpackLayer(renderer, modelSet));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<AvatarRenderer<?>>() {}, (avatar, state) -> {
            var data = avatar.getExistingDataOrNull(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
            var warning = avatar.getExistingDataOrNull(FrenchieRegistry.FRENCHIE_WARNING);
            state.setRenderData(ALERT_YAW, warning != null && warning.visible(avatar.level().getGameTime())
                    ? Mth.clamp(Mth.wrapDegrees(warning.bearing() - state.bodyRot), -60F, 60F) : null);
            state.setRenderData(FrontpackLayer.CARRIED_COAT, data == null ? null : data.carriedCoat());
            var pack = avatar.getOffhandItem();
            if (!pack.is(FrenchieRegistry.FRONT_PACK.get())) pack = avatar.getItemBySlot(EquipmentSlot.CHEST);
            var dye = pack.is(FrenchieRegistry.FRONT_PACK.get()) ? pack.get(DataComponents.DYED_COLOR) : null;
            state.setRenderData(FrontpackLayer.FABRIC_TINT, dye == null ? null : FrontpackTint.fabricColor(dye.rgb()));
        });
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        var data = player.getExistingDataOrNull(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
        if (player.isShiftKeyDown() && data != null && data.hasCarriedFrenchie()) {
            ClientPacketDistributor.sendToServer(PutDownFrenchiePayload.INSTANCE);
        }
    }
}
