package net.j40climb.frenchies.test;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class CompanionTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> defaultEnv = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf());
        var yard = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "companion_yard");
        registerStructureTest(event, defaultEnv, "companion_fetch", yard, 550, CompanionTravelTests::fetch);
        registerStructureTest(event, defaultEnv, "companion_captain", yard, 550, CompanionTravelTests::captain);
        registerStructureTest(event, defaultEnv, "companion_zoomies_interrupt", yard, 550, CompanionTravelTests::interruptCelebration);
        registerStructureTest(event, defaultEnv, "companion_audience", yard, 550, CompanionTravelTests::audience);
        registerStructureTest(event, defaultEnv, "companion_zoomies_laps", yard, 550, h -> CompanionTravelTests.zoomies(h, true));
        registerStructureTest(event, defaultEnv, "companion_zoomies_crops", yard, 550, h -> CompanionTravelTests.zoomies(h, false));
        registerTest(event, defaultEnv, "frenchie_warning_sight", FrenchieAwarenessGameTests::warningSight);
        registerTest(event, defaultEnv, "frenchie_bedtime", FrenchieAwarenessGameTests::bedtime);
        registerTest(event, defaultEnv, "frenchie_owner_orders", FrenchieGameTests::ownerOrders);
        registerTest(event, defaultEnv, "frenchie_death_trail", FrenchieTrackingGameTests::trackDeath);
        registerTest(event, defaultEnv, "frenchie_delivery_creative_parcel", FrenchieDeliveryGameTests::creativeParcel);
        registerTest(event, defaultEnv, "frenchie_compass_orders", FrenchieDeliveryGameTests::compassOrders);
        registerStructureTest(event, defaultEnv, "frenchie_home_recall",
                FrenchiePen.STRUCTURE, 3000, FrenchieRecallGameTests::recallParkedDog);
        registerTest(event, defaultEnv, "frenchie_home_bed_interaction", FrenchieRecallGameTests::sendUsingBed);
        registerTest(event, defaultEnv, "frenchie_delivery_packed_cancel", FrenchieDeliveryGameTests::packAndCancel);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_recipient_full",
                FrenchiePen.STRUCTURE, 900, FrenchieDeliveryGameTests::fullRecipient);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_recipient_disconnect",
                FrenchiePen.STRUCTURE, 500, FrenchieDeliveryGameTests::disconnectedRecipient);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_cross_dimension",
                FrenchiePen.STRUCTURE, 2400, FrenchieDeliveryGameTests::crossDimension);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_blocked_departure",
                FrenchiePen.STRUCTURE, 650, FrenchieDeliveryGameTests::blockedDeparture);
        registerTest(event, defaultEnv, "frenchie_delivery_legacy_bed_pickup", FrenchieDeliveryGameTests::legacyBedPickup);
        registerTest(event, defaultEnv, "frenchie_delivery_respawn_preview", FrenchieBedPreviewGameTest::respawnPreview);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_unloaded_bed",
                FrenchiePen.STRUCTURE, 1800, FrenchieDeliveryGameTests::unloadedHomeBed);
        registerTest(event, defaultEnv, "frenchie_frontpack_respawn_release", FrenchieGameTests::frontpackRespawnRelease);
        registerTest(event, defaultEnv, "frenchie_frontpack_unequip_release", FrenchieDeliveryGameTests::unequipPackedCourier);
        registerTest(event, defaultEnv, "frenchie_frontpack_appearance", FrenchieGameTests::frontpackAppearance);
        registerTest(event, defaultEnv, "frenchie_frontpack_right_click_equip", FrontpackEquipGameTest::equipFromHand);
        registerTest(event, defaultEnv, "frenchie_frontpack_dye", FrontpackDyeGameTest::craftAndWash);
        registerTest(event, defaultEnv, "frenchie_frontpack_elytra", FrontpackElytraGameTest::craftAndRepair);
    }

    static void registerStructureTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> env,
            String name,
            Identifier structure,
            int maxTicks,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                env,
                structure,
                maxTicks,
                20,   // setupTicks - give block entities time to load
                true  // required
        );

        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }

    /**
     * Register a test with the default empty structure and settings.
     */
    static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> env,
            String name,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                env,
                EMPTY_STRUCTURE,
                100, // maxTicks
                0,   // setupTicks
                true  // required
        );

        // Create a SimpleGameTestInstance wrapper
        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }
}
