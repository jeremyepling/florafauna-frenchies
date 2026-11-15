package net.j40climb.frenchies;

import net.j40climb.frenchies.common.entity.frenchie.DeliveryDialog;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieCommands;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.test.CompanionTests;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Arrays;

@Mod(Frenchies.MOD_ID)
public final class Frenchies {
    public static final String MOD_ID = "florafauna_frenchies";
    // Existing worlds refer to these resource keys even when the owning mod changes.
    public static final String LEGACY_NAMESPACE = "florafauna";

    public Frenchies(IEventBus bus) {
        FrenchieRegistry.register(bus);
        bus.addListener(Frenchies::networking);
        bus.addListener(Frenchies::attributes);
        bus.addListener(Frenchies::creativeTabs);
        NeoForge.EVENT_BUS.addListener(Frenchies::commands);
        if (Arrays.asList(System.getProperty("neoforge.enabledGameTestNamespaces", "").split(",")).contains(MOD_ID)) {
            bus.addListener(CompanionTests::register);
        }
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(FrenchieRegistry.FRENCHIE.get(), FrenchieEntity.createAttributes().build());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(FrenchieRegistry.FRENCHIE_SPAWN_EGG);
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FrenchieRegistry.FRONT_PACK);
            event.accept(FrenchieRegistry.WINGED_FRONT_PACK);
        }
    }

    private static void commands(RegisterCommandsEvent event) {
        DeliveryDialog.register(event.getDispatcher());
        FrenchieCommands.register(event.getDispatcher());
    }

    private static void networking(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC, PutDownFrenchiePayload::onServerReceived);
        registrar.versioned("2").playToClient(DeliveryPopupPayload.TYPE, DeliveryPopupPayload.STREAM_CODEC);
    }
}
