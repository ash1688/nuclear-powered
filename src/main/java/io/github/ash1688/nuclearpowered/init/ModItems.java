package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import net.minecraft.world.item.Item;
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
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DEPLETED_URANIUM_FUEL_ROD = ITEMS.register("depleted_uranium_fuel_rod",
            () -> new Item(new Item.Properties().stacksTo(16)));

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

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
