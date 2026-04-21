package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

@JeiPlugin
public class NuclearPoweredJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = new ResourceLocation(NuclearPowered.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CrushingRecipeCategory(helper));
        registration.addRecipeCategories(new WashingRecipeCategory(helper));
        registration.addRecipeCategories(new FabricatingRecipeCategory(helper));
        registration.addRecipeCategories(new ShearingCategory(helper));
        registration.addRecipeCategories(new DissolvingCategory(helper));
        registration.addRecipeCategories(new ExtractingCategory(helper));
        registration.addRecipeCategories(new CsColumnCategory(helper));
        registration.addRecipeCategories(new VitrifyingCategory(helper));
        registration.addRecipeCategories(new CladdingRecyclingCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RecipeManager rm = level.getRecipeManager();

        // JSON-defined recipe categories — pulled from the recipe manager.
        registration.addRecipes(CrushingRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get()));
        registration.addRecipes(WashingRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipes.WASHING_TYPE.get()));
        registration.addRecipes(FabricatingRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipes.FABRICATING_TYPE.get()));

        // Reprocessing chain — recipes are hardcoded in each machine's block
        // entity, so mirror them here as JEI display entries.
        registration.addRecipes(ShearingCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(10_000, 200,
                        List.of(new ItemStack(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())),
                        List.of(new ItemStack(ModItems.CHOPPED_FUEL.get()),
                                new ItemStack(ModItems.CLADDING_SCRAP.get())))));

        registration.addRecipes(DissolvingCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(30_000, 200,
                        new FluidStack(ModFluids.NITRIC_ACID.get(), 250),
                        List.of(new ItemStack(ModItems.CHOPPED_FUEL.get())),
                        List.of(new ItemStack(ModItems.DISSOLVED_FUEL.get()),
                                new ItemStack(ModItems.REACTOR_SLUDGE.get())))));

        registration.addRecipes(ExtractingCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(50_000, 200,
                        new FluidStack(ModFluids.EXTRACTION_SOLVENT.get(), 250),
                        List.of(new ItemStack(ModItems.DISSOLVED_FUEL.get())),
                        List.of(new ItemStack(ModItems.PLUTONIUM_239.get()),
                                new ItemStack(ModItems.RECLAIMED_URANIUM.get()),
                                new ItemStack(ModItems.FISSION_PRODUCT_STREAM.get())))));

        registration.addRecipes(CsColumnCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(70_000, 200,
                        List.of(new ItemStack(ModItems.FISSION_PRODUCT_STREAM.get()),
                                new ItemStack(ModItems.ION_EXCHANGE_RESIN.get())),
                        List.of(new ItemStack(ModItems.CESIUM_137.get()),
                                new ItemStack(ModItems.RESIDUAL_WASTE.get())))));

        registration.addRecipes(VitrifyingCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(80_000, 200,
                        List.of(new ItemStack(ModItems.RESIDUAL_WASTE.get()),
                                new ItemStack(ModItems.GLASS_FRIT.get())),
                        List.of(new ItemStack(ModItems.VITRIFIED_WASTE.get())))));

        registration.addRecipes(CladdingRecyclingCategory.RECIPE_TYPE, List.of(
                ReprocessingRecipe.of(20_000, 200,
                        List.of(new ItemStack(ModItems.CLADDING_SCRAP.get(), 9)),
                        List.of(new ItemStack(ModItems.FUEL_ROD_CLADDING.get())))));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()),
                CrushingRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WASHER.get()),
                WashingRecipeCategory.RECIPE_TYPE);
        // Electric furnace runs vanilla smelting recipes, so clicking it in JEI
        // shows the full smelting list (including our custom ones).
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()),
                RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FUEL_FABRICATOR.get()),
                FabricatingRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHEARER.get()),
                ShearingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DISSOLVER.get()),
                DissolvingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EXTRACTION_COLUMN.get()),
                ExtractingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CS_COLUMN.get()),
                CsColumnCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.VITRIFIER.get()),
                VitrifyingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CLADDING_RECYCLER.get()),
                CladdingRecyclingCategory.RECIPE_TYPE);
    }
}
