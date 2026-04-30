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
                        out.accept(ModBlocks.GRAPHITE_PILE_CONTROLLER.get());
                        out.accept(ModBlocks.GRAPHITE_CASING.get());
                        out.accept(ModBlocks.FUEL_ROD_OUTPUT_PORT.get());
                        out.accept(ModBlocks.THERMOCOUPLE.get());
                        out.accept(ModBlocks.ENERGY_CABLE.get());
                        out.accept(ModBlocks.BATTERY.get());
                        out.accept(ModBlocks.ENERGY_CONVERTER.get());
                        out.accept(ModBlocks.COAL_BOILER.get());
                        out.accept(ModBlocks.STEAM_ENGINE.get());
                        out.accept(ModBlocks.STEAM_PIPE.get());
                        out.accept(ModBlocks.HEATER.get());
                        out.accept(ModBlocks.INFINITE_WATER_SOURCE.get());
                        out.accept(ModBlocks.INFINITE_NITRIC_ACID_SOURCE.get());
                        out.accept(ModBlocks.INFINITE_SOLVENT_SOURCE.get());
                        out.accept(ModBlocks.COOLING_POND.get());
                        out.accept(ModItems.HOT_SPENT_FUEL_ROD.get());
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
                        out.accept(ModItems.MIXED_ACTINIDES.get());
                        out.accept(ModItems.RECLAIMED_URANIUM.get());
                        out.accept(ModItems.PLUTONIUM_239.get());
                        out.accept(ModItems.CESIUM_137.get());
                        out.accept(ModItems.VITRIFIED_WASTE.get());
                        out.accept(ModItems.GLASS_FRIT.get());
                        out.accept(ModItems.ION_EXCHANGE_RESIN.get());
                        out.accept(ModItems.FERMI_III_COIN.get());
                        // Fermi-III Exchange shop catalog
                        out.accept(ModItems.MAGNOX_BLUEPRINT.get());
                        out.accept(ModItems.RBMK_BLUEPRINT.get());
                        out.accept(ModItems.CRUSHER_SPEED_CARD.get());
                        out.accept(ModItems.WASHER_SPEED_CARD.get());
                        out.accept(ModItems.FURNACE_SPEED_CARD.get());
                        out.accept(ModItems.FABRICATOR_SPEED_CARD.get());
                        out.accept(ModItems.SHEARER_SPEED_CARD.get());
                        out.accept(ModItems.DISSOLVER_REAGENT_SAVER.get());
                        out.accept(ModItems.EXTRACTION_SOLVENT_SAVER.get());
                        out.accept(ModItems.CS_RESIN_SAVER.get());
                        out.accept(ModItems.CLADDING_COMPACTOR.get());
                        out.accept(ModItems.EXTENDED_BURN_MODULE.get());
                        out.accept(ModItems.HEAT_CAPTURE_EFFICIENCY_CORE.get());
                        out.accept(ModItems.THERMAL_DAMPENER.get());
                        out.accept(ModItems.URANIUM_STARTER_CACHE.get());
                        out.accept(ModItems.REAGENT_BUNDLE.get());
                        // New machines + component blocks.
                        out.accept(ModBlocks.STAMPING_PRESS.get());
                        out.accept(ModBlocks.MACERATOR.get());
                        out.accept(ModBlocks.SLICER.get());
                        out.accept(ModBlocks.WIREMILL.get());
                        out.accept(ModBlocks.RUBBER_SQUEEZER.get());
                        out.accept(ModBlocks.TANK.get());
                        out.accept(ModBlocks.VAT.get());
                        out.accept(ModBlocks.FLUID_PIPE.get());
                        // Component tier — frames, cores, sub-assemblies, intermediates.
                        out.accept(ModItems.COAL_BOILER_FRAME.get());
                        out.accept(ModItems.MACHINE_FRAME.get());
                        out.accept(ModItems.MACHINE_FRAME_T2.get());
                        out.accept(ModItems.MACHINE_FRAME_T3.get());
                        out.accept(ModItems.PILE_FRAME.get());
                        out.accept(ModItems.STEAM_ENGINE_FRAME.get());
                        out.accept(ModItems.BATTERY_CORE.get());
                        out.accept(ModItems.THERMO_CORE.get());
                        out.accept(ModItems.PUMP.get());
                        out.accept(ModItems.CIRCUIT.get());
                        out.accept(ModItems.ELECTRIC_CIRCUIT.get());
                        out.accept(ModItems.ELECTRO_MAGNET.get());
                        out.accept(ModItems.POWER_CELL.get());
                        out.accept(ModItems.SMALL_GENERATOR.get());
                        out.accept(ModItems.HEATING_COIL.get());
                        out.accept(ModItems.HEATING_ELEMENT.get());
                        out.accept(ModItems.IRON_SHEAR.get());
                        out.accept(ModItems.VICE.get());
                        out.accept(ModItems.JAGGED_FLINT.get());
                        out.accept(ModItems.WATER_JET.get());
                        out.accept(ModItems.NOZZLE.get());
                        out.accept(ModItems.WRENCH.get());
                        out.accept(ModItems.IRON_GRATE.get());
                        out.accept(ModItems.FUEL_ASSEMBLY.get());
                        out.accept(ModItems.WHEEL.get());
                        out.accept(ModItems.RUBBER_RING.get());
                        out.accept(ModItems.COPPER_PIPE.get());
                        out.accept(ModItems.SILICON.get());
                        out.accept(ModItems.SILICON_BASE.get());
                        out.accept(ModItems.PCB.get());
                        out.accept(ModItems.RUBBER.get());
                        out.accept(ModItems.EMPTY_FUEL_ROD.get());
                        out.accept(ModItems.EMPTY_FUEL_ROD_T2.get());
                        // Alloy fallbacks (forge:ingots/<name>, plates/<name>, wires/<name>)
                        out.accept(ModItems.BRONZE_INGOT.get());
                        out.accept(ModItems.BRONZE_PLATE.get());
                        out.accept(ModItems.CONSTANTAN_INGOT.get());
                        out.accept(ModItems.CONSTANTAN_PLATE.get());
                        out.accept(ModItems.ELECTRUM_INGOT.get());
                        out.accept(ModItems.ELECTRUM_PLATE.get());
                        out.accept(ModItems.ELECTRUM_WIRE.get());
                        out.accept(ModItems.DENSE_STEEL_INGOT.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
