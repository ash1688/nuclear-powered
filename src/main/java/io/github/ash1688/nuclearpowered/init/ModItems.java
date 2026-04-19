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

    public static final RegistryObject<Item> PURIFIED_URANIUM = ITEMS.register("purified_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRUSHED_THORIUM = ITEMS.register("crushed_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PURIFIED_THORIUM = ITEMS.register("purified_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> URANIUM_FUEL_ROD = ITEMS.register("uranium_fuel_rod",
            () -> new Item(new Item.Properties().stacksTo(16)));

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
