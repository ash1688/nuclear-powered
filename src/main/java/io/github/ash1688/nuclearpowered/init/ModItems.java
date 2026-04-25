package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.item.HotSpentFuelRodItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NuclearPowered.MODID);

    public static final RegistryObject<Item> RAW_URANIUM = ITEMS.register("raw_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAW_THORIUM = ITEMS.register("raw_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRUSHED_URANIUM = ITEMS.register("crushed_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> YELLOWCAKE = ITEMS.register("yellowcake",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRUSHED_THORIUM = ITEMS.register("crushed_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> THORIUM_CONCENTRATE = ITEMS.register("thorium_concentrate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> URANIUM_FUEL_ROD = ITEMS.register("uranium_fuel_rod",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DEPLETED_URANIUM_FUEL_ROD = ITEMS.register("depleted_uranium_fuel_rod",
            () -> new Item(new Item.Properties()));

    // Fresh pile output — too hot to handle. Must pass through a Cooling Pond
    // before it can be shered for reprocessing.
    public static final RegistryObject<Item> HOT_SPENT_FUEL_ROD = ITEMS.register("hot_spent_fuel_rod",
            () -> new HotSpentFuelRodItem(new Item.Properties()));

    // --- Tier 1 PUREX reprocessing chain ---
    // Intermediates produced along the chain. Kept as items (not fluids) except
    // for the two reagents (nitric acid + extraction solvent) to keep the piping
    // story simpler for Tier 1.
    public static final RegistryObject<Item> CHOPPED_FUEL = ITEMS.register("chopped_fuel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLADDING_SCRAP = ITEMS.register("cladding_scrap",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DISSOLVED_FUEL = ITEMS.register("dissolved_fuel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REACTOR_SLUDGE = ITEMS.register("reactor_sludge",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FISSION_PRODUCT_STREAM = ITEMS.register("fission_product_stream",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESIDUAL_WASTE = ITEMS.register("residual_waste",
            () -> new Item(new Item.Properties()));

    // GT-path only intermediate: the combined U + Pu stream that comes out of
    // a GT Chemical Reactor's first extraction pass. NP's native Extraction
    // Column splits Pu and reclaimed U in a single step (3-output recipe),
    // but GT's single-block Chemical Reactor caps at 2 outputs, so the GT
    // route runs a second partitioning pass that turns this mix into the
    // separate plutonium_239 + reclaimed_uranium. Matches real PUREX
    // chemistry, which also does partitioning as a distinct step after the
    // initial solvent extraction.
    public static final RegistryObject<Item> MIXED_ACTINIDES = ITEMS.register("mixed_actinides",
            () -> new Item(new Item.Properties()));

    // Final outputs + reagents
    public static final RegistryObject<Item> RECLAIMED_URANIUM = ITEMS.register("reclaimed_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLUTONIUM_239 = ITEMS.register("plutonium_239",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CESIUM_137 = ITEMS.register("cesium_137",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VITRIFIED_WASTE = ITEMS.register("vitrified_waste",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GLASS_FRIT = ITEMS.register("glass_frit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ION_EXCHANGE_RESIN = ITEMS.register("ion_exchange_resin",
            () -> new Item(new Item.Properties()));

    // Recycled fuel rod cladding (9 scrap compacted back into a reusable shell).
    // Accepted by the fuel fabricator as a drop-in for the 4 iron ingots.
    public static final RegistryObject<Item> FUEL_ROD_CLADDING = ITEMS.register("fuel_rod_cladding",
            () -> new Item(new Item.Properties()));

    // Fermi-III quest-book currency. Awarded by the bundled FTB Quests chapter;
    // spent in the Fermi-III Exchange shop chapter. Tier-specific — later
    // releases add T2/T3/T4 coins; unlocking the Tier 5 SMR controller will
    // eventually require 1 of each tier coin.
    public static final RegistryObject<Item> FERMI_III_COIN = ITEMS.register("fermi_iii_coin",
            () -> new Item(new Item.Properties()));

    // --- Fermi-III Exchange shop catalog (Phase 1: items are inert; effects
    // wired in Phases 2/3 as laid out in the exchange plan). ---

    // T2 reactor controller blueprints — owning one unlocks that T2 reactor's
    // crafting recipe when T2 scaffolding lands.
    public static final RegistryObject<Item> MAGNOX_BLUEPRINT = ITEMS.register("magnox_blueprint",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> RBMK_BLUEPRINT = ITEMS.register("rbmk_blueprint",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Machine upgrade cards — slotted into the target machine's upgrade bay.
    public static final RegistryObject<Item> CRUSHER_SPEED_CARD = ITEMS.register("crusher_speed_card",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> WASHER_SPEED_CARD = ITEMS.register("washer_speed_card",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FURNACE_SPEED_CARD = ITEMS.register("furnace_speed_card",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FABRICATOR_SPEED_CARD = ITEMS.register("fabricator_speed_card",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SHEARER_SPEED_CARD = ITEMS.register("shearer_speed_card",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DISSOLVER_REAGENT_SAVER = ITEMS.register("dissolver_reagent_saver",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> EXTRACTION_SOLVENT_SAVER = ITEMS.register("extraction_solvent_saver",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CS_RESIN_SAVER = ITEMS.register("cs_resin_saver",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CLADDING_COMPACTOR = ITEMS.register("cladding_compactor",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // Permanent pile / thermo upgrades — consumed on right-click, stamp a
    // boolean flag into the target BE's NBT.
    public static final RegistryObject<Item> EXTENDED_BURN_MODULE = ITEMS.register("extended_burn_module",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> HEAT_CAPTURE_EFFICIENCY_CORE = ITEMS.register("heat_capture_efficiency_core",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> THERMAL_DAMPENER = ITEMS.register("thermal_dampener",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // Utility kits — right-click eventually drops their bundled contents;
    // currently inert placeholders until Phase 2 lands.
    public static final RegistryObject<Item> URANIUM_STARTER_CACHE = ITEMS.register("uranium_starter_cache",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> REAGENT_BUNDLE = ITEMS.register("reagent_bundle",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // Filled buckets for the reprocessing reagents. Registered here rather than
    // inside ModFluids so we can reference the Items registry cleanly; the
    // ForgeFlowingFluid.Properties in ModFluids then points back at these via
    // a supplier to close the loop.
    public static final RegistryObject<BucketItem> NITRIC_ACID_BUCKET = ITEMS.register("nitric_acid_bucket",
            () -> new BucketItem(ModFluids.NITRIC_ACID,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> EXTRACTION_SOLVENT_BUCKET = ITEMS.register("extraction_solvent_bucket",
            () -> new BucketItem(ModFluids.EXTRACTION_SOLVENT,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
