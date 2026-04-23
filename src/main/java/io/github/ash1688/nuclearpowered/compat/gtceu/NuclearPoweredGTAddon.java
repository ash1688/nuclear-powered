package io.github.ash1688.nuclearpowered.compat.gtceu;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mojang.logging.LogUtils;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import io.github.ash1688.nuclearpowered.init.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * GregTech CEu integration: registers Nuclear Powered's processing and
 * reprocessing recipes into GT's own machines so a GT-heavy pack can run the
 * full NP fuel cycle with the Macerator, Ore Washer, Electric Furnace,
 * Assembler, Chemical Reactor, Centrifuge, Compressor, and Cutter instead of
 * the NP-native machines.
 *
 * <p>This class is annotated {@link GTAddon} and implements {@link IGTAddon};
 * GT's {@code AddonFinder} discovers it only when GT itself is loaded, so
 * nothing here ever runs in a non-GT pack. The class file still sits in the
 * NP jar but its bytecode is never linked without GT on the classpath —
 * {@code @GTAddon} is just an annotation reference, scanned as raw metadata
 * by Forge's {@code ModFileScanData} without classloading the target.</p>
 *
 * <p>All recipes are keyed under {@code nuclearpowered:gtceu/<machine>/<name>}
 * so they're visible as NP-owned in JEI and don't collide with GT's own IDs.
 * EU/t + duration figures track the Phase C plan table: LV-tier ore chain
 * (8 EU/t), MV Assembler + initial chemistry (32 / 128 EU/t), HV late-chain
 * extraction / centrifuge / vitrification (256 EU/t).</p>
 */
@GTAddon
public class NuclearPoweredGTAddon implements IGTAddon {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final GTRegistrate REGISTRATE = GTRegistrate.create(NuclearPowered.MODID);

    public NuclearPoweredGTAddon() {
        LOGGER.info("[NP/GT] NuclearPoweredGTAddon instantiated — @GTAddon discovery is working.");
    }

    @Override
    public GTRegistrate getRegistrate() {
        return REGISTRATE;
    }

    @Override
    public String addonModId() {
        return NuclearPowered.MODID;
    }

    @Override
    public void initializeAddon() {
        LOGGER.info("[NP/GT] initializeAddon() called.");
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> consumer) {
        LOGGER.info("[NP/GT] addRecipes() starting — registering NP cross-mod recipes into GT machines.");
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        // Wrap the caller's consumer so we can count successes and catch any
        // per-recipe exception that GT might silently swallow.
        Consumer<FinishedRecipe> counting = recipe -> {
            try {
                consumer.accept(recipe);
                ok.incrementAndGet();
            } catch (Throwable t) {
                failed.incrementAndGet();
                LOGGER.error("[NP/GT] consumer rejected recipe {}", recipe.getId(), t);
            }
        };
        try {
            addOreProcessingRecipes(counting);
            addReprocessingRecipes(counting);
        } catch (Throwable t) {
            LOGGER.error("[NP/GT] addRecipes() threw — recipe registration aborted partway", t);
        }
        LOGGER.info("[NP/GT] addRecipes() done. registered={}, failed={}", ok.get(), failed.get());
    }

    // ------------------------------------------------------------------
    //  Ore-processing chain — LV tier throughout. A steam-age GT player
    //  can run raw_uranium -> crushed -> yellowcake -> uranium_ingot
    //  without ever placing an NP-native Crusher / Washer / Electric
    //  Furnace, using Macerator + Ore Washer + Electric Furnace instead.
    // ------------------------------------------------------------------

    private void addOreProcessingRecipes(Consumer<FinishedRecipe> c) {
        macerate("raw_uranium_to_crushed", ModItems.RAW_URANIUM.get(),
                ModItems.CRUSHED_URANIUM.get(), 80, 8, c);
        macerate("raw_thorium_to_crushed", ModItems.RAW_THORIUM.get(),
                ModItems.CRUSHED_THORIUM.get(), 80, 8, c);

        // Ore Washer in GT CEu expects a water fluid input; 100 mB per batch
        // is the standard for single-ingot washes.
        oreWash("crushed_uranium_to_yellowcake", ModItems.CRUSHED_URANIUM.get(),
                ModItems.YELLOWCAKE.get(), 200, 8, c);
        oreWash("crushed_thorium_to_concentrate", ModItems.CRUSHED_THORIUM.get(),
                ModItems.THORIUM_CONCENTRATE.get(), 200, 8, c);

        smelt("yellowcake_to_uranium_ingot", ModItems.YELLOWCAKE.get(),
                ModItems.URANIUM_INGOT.get(), 200, 8, c);
    }

    // ------------------------------------------------------------------
    //  Reprocessing chain — MV/HV, mirrors the NP PUREX flow. Assembler
    //  replaces the Fuel Fabricator, Chemical Reactor covers Dissolver and
    //  both Extraction Column outputs + Vitrifier, Centrifuge handles the
    //  Cs column, Compressor compacts cladding scraps, Cutter shears
    //  depleted rods into chopped fuel + scrap.
    // ------------------------------------------------------------------

    private void addReprocessingRecipes(Consumer<FinishedRecipe> c) {
        // Assembler — two parallel fuel-rod recipes (4 × iron plate
        // or 1 × recycled cladding), matching the NP Fuel Fabricator's
        // dual inputs. 32 EU/t × 200 ticks at MV.
        GTRecipeBuilder.of(id("assembler/uranium_fuel_rod_iron"), GTRecipeTypes.ASSEMBLER_RECIPES)
                .inputItems(ModItems.URANIUM_INGOT.get(), 3)
                .inputItems(net.minecraft.world.item.Items.IRON_INGOT, 4)
                .outputItems(ModItems.URANIUM_FUEL_ROD.get())
                .duration(200).EUt(32).save(c);

        GTRecipeBuilder.of(id("assembler/uranium_fuel_rod_recycled"), GTRecipeTypes.ASSEMBLER_RECIPES)
                .inputItems(ModItems.URANIUM_INGOT.get(), 3)
                .inputItems(ModItems.FUEL_ROD_CLADDING.get())
                .outputItems(ModItems.URANIUM_FUEL_ROD.get())
                .duration(200).EUt(32).save(c);

        // Cutter — depleted rod -> chopped fuel + cladding scrap. Replaces
        // the Shearer. Dual outputs; each represents one of the two item
        // products from the NP shearer.
        GTRecipeBuilder.of(id("cutter/depleted_rod"), GTRecipeTypes.CUTTER_RECIPES)
                .inputItems(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())
                .outputItems(ModItems.CHOPPED_FUEL.get())
                .outputItems(ModItems.CLADDING_SCRAP.get())
                .duration(200).EUt(16).save(c);

        // Compressor — 9 × cladding_scrap -> fuel_rod_cladding. Mirror of
        // the NP Cladding Recycler.
        GTRecipeBuilder.of(id("compressor/fuel_rod_cladding"), GTRecipeTypes.COMPRESSOR_RECIPES)
                .inputItems(ModItems.CLADDING_SCRAP.get(), 9)
                .outputItems(ModItems.FUEL_ROD_CLADDING.get())
                .duration(200).EUt(8).save(c);

        // Chemical Reactor — dissolution step. 250 mB nitric acid + chopped
        // fuel -> dissolved fuel + reactor sludge. MV-tier chemistry.
        GTRecipeBuilder.of(id("chemical_reactor/dissolution"), GTRecipeTypes.CHEMICAL_RECIPES)
                .inputItems(ModItems.CHOPPED_FUEL.get())
                .inputFluids(new FluidStack(ModFluids.NITRIC_ACID.get(), 250))
                .outputItems(ModItems.DISSOLVED_FUEL.get())
                .outputItems(ModItems.REACTOR_SLUDGE.get())
                .duration(400).EUt(128).save(c);

        // Chemical Reactor — extraction step. Collapses NP's three-way
        // extraction column output (Pu + U + fission-product stream) into
        // a single HV-tier chemical recipe; GT recipes support multiple
        // item outputs natively.
        GTRecipeBuilder.of(id("chemical_reactor/extraction"), GTRecipeTypes.CHEMICAL_RECIPES)
                .inputItems(ModItems.DISSOLVED_FUEL.get())
                .inputFluids(new FluidStack(ModFluids.EXTRACTION_SOLVENT.get(), 250))
                .outputItems(ModItems.PLUTONIUM_239.get())
                .outputItems(ModItems.RECLAIMED_URANIUM.get())
                .outputItems(ModItems.FISSION_PRODUCT_STREAM.get())
                .duration(600).EUt(256).save(c);

        // Centrifuge — Cs separation. fission_product_stream + ion exchange
        // resin -> cesium_137 + residual_waste. Resin is a shop-sourced
        // consumable; GT players buy it or craft via NP's reagent chain.
        GTRecipeBuilder.of(id("centrifuge/cesium"), GTRecipeTypes.CENTRIFUGE_RECIPES)
                .inputItems(ModItems.FISSION_PRODUCT_STREAM.get())
                .inputItems(ModItems.ION_EXCHANGE_RESIN.get())
                .outputItems(ModItems.CESIUM_137.get())
                .outputItems(ModItems.RESIDUAL_WASTE.get())
                .duration(600).EUt(256).save(c);

        // Chemical Reactor — vitrification. residual_waste + glass_frit ->
        // vitrified_waste. Final stabilised output for long-term storage.
        GTRecipeBuilder.of(id("chemical_reactor/vitrification"), GTRecipeTypes.CHEMICAL_RECIPES)
                .inputItems(ModItems.RESIDUAL_WASTE.get())
                .inputItems(ModItems.GLASS_FRIT.get())
                .outputItems(ModItems.VITRIFIED_WASTE.get())
                .duration(600).EUt(256).save(c);
    }

    // ------------------------------------------------------------------
    //  Small helpers — reduce builder noise for the homogeneous
    //  ore-processing recipes.
    // ------------------------------------------------------------------

    private static void macerate(String name, Item in, Item out, int duration, int eut,
                                  Consumer<FinishedRecipe> c) {
        GTRecipeBuilder.of(id("macerator/" + name), GTRecipeTypes.MACERATOR_RECIPES)
                .inputItems(in)
                .outputItems(out)
                .duration(duration).EUt(eut).save(c);
    }

    private static void oreWash(String name, Item in, Item out, int duration, int eut,
                                 Consumer<FinishedRecipe> c) {
        GTRecipeBuilder.of(id("ore_washer/" + name), GTRecipeTypes.ORE_WASHER_RECIPES)
                .inputItems(in)
                .inputFluids(new FluidStack(Fluids.WATER, 100))
                .outputItems(out)
                .duration(duration).EUt(eut).save(c);
    }

    private static void smelt(String name, Item in, Item out, int duration, int eut,
                               Consumer<FinishedRecipe> c) {
        GTRecipeBuilder.of(id("electric_furnace/" + name), GTRecipeTypes.FURNACE_RECIPES)
                .inputItems(in)
                .outputItems(out)
                .duration(duration).EUt(eut).save(c);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NuclearPowered.MODID, "gtceu/" + path);
    }
}
