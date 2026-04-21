package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ExtractingCategory extends ReprocessingCategoryBase {
    public static final RecipeType<ReprocessingRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(NuclearPowered.MODID, "extracting"), ReprocessingRecipe.class);

    public ExtractingCategory(IGuiHelper helper) {
        super(helper, new ItemStack(ModBlocks.EXTRACTION_COLUMN.get()),
                Component.translatable("block.nuclearpowered.extraction_column"));
    }

    @Override public RecipeType<ReprocessingRecipe> getRecipeType() { return RECIPE_TYPE; }
}
