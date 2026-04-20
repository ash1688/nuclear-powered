package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NuclearPowered.MODID);

    public static final RegistryObject<CreativeModeTab> NUCLEAR_TAB = CREATIVE_MODE_TABS.register("nuclear_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + NuclearPowered.MODID))
                    .icon(() -> ModItems.RAW_URANIUM.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(ModBlocks.URANIUM_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
                        out.accept(ModBlocks.NETHER_URANIUM_ORE.get());
                        out.accept(ModBlocks.END_URANIUM_ORE.get());
                        out.accept(ModItems.RAW_URANIUM.get());
                        out.accept(ModItems.CRUSHED_URANIUM.get());
                        out.accept(ModItems.YELLOWCAKE.get());
                        out.accept(ModItems.URANIUM_INGOT.get());
                        out.accept(ModBlocks.THORIUM_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_THORIUM_ORE.get());
                        out.accept(ModBlocks.NETHER_THORIUM_ORE.get());
                        out.accept(ModBlocks.END_THORIUM_ORE.get());
                        out.accept(ModItems.RAW_THORIUM.get());
                        out.accept(ModItems.CRUSHED_THORIUM.get());
                        out.accept(ModItems.THORIUM_CONCENTRATE.get());
                        out.accept(ModItems.URANIUM_FUEL_ROD.get());
                        out.accept(ModItems.DEPLETED_URANIUM_FUEL_ROD.get());
                        out.accept(ModBlocks.CRUSHER.get());
                        out.accept(ModBlocks.WASHER.get());
                        out.accept(ModBlocks.ELECTRIC_FURNACE.get());
                        out.accept(ModBlocks.FUEL_FABRICATOR.get());
                        out.accept(ModBlocks.GRAPHITE_PILE.get());
                        out.accept(ModBlocks.GRAPHITE_CASING.get());
                        out.accept(ModBlocks.THERMOCOUPLE.get());
                        out.accept(ModBlocks.ENERGY_CABLE.get());
                        out.accept(ModBlocks.BATTERY.get());
                        out.accept(ModBlocks.COAL_BOILER.get());
                        out.accept(ModBlocks.STEAM_ENGINE.get());
                        out.accept(ModBlocks.STEAM_PIPE.get());
                        out.accept(ModBlocks.HEATER.get());
                        out.accept(ModBlocks.SHEARER.get());
                        out.accept(ModBlocks.DISSOLVER.get());
                        out.accept(ModBlocks.EXTRACTION_COLUMN.get());
                        out.accept(ModBlocks.CS_COLUMN.get());
                        out.accept(ModBlocks.VITRIFIER.get());
                        out.accept(ModBlocks.CLADDING_RECYCLER.get());
                        out.accept(ModItems.FUEL_ROD_CLADDING.get());
                        out.accept(ModItems.NITRIC_ACID_BUCKET.get());
                        out.accept(ModItems.EXTRACTION_SOLVENT_BUCKET.get());
                        // Tier 1 reprocessing items + reagents
                        out.accept(ModItems.CHOPPED_FUEL.get());
                        out.accept(ModItems.CLADDING_SCRAP.get());
                        out.accept(ModItems.DISSOLVED_FUEL.get());
                        out.accept(ModItems.REACTOR_SLUDGE.get());
                        out.accept(ModItems.FISSION_PRODUCT_STREAM.get());
                        out.accept(ModItems.RESIDUAL_WASTE.get());
                        out.accept(ModItems.RECLAIMED_URANIUM.get());
                        out.accept(ModItems.PLUTONIUM_239.get());
                        out.accept(ModItems.CESIUM_137.get());
                        out.accept(ModItems.VITRIFIED_WASTE.get());
                        out.accept(ModItems.GLASS_FRIT.get());
                        out.accept(ModItems.ION_EXCHANGE_RESIN.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
