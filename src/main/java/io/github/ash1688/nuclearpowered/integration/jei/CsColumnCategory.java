package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CsColumnCategory extends ReprocessingCategoryBase {
    public static final RecipeType<ReprocessingRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(NuclearPowered.MODID, "cs_column"), ReprocessingRecipe.class);

    public CsColumnCategory(IGuiHelper helper) {
        super(helper, new ItemStack(ModBlocks.CS_COLUMN.get()),
                Component.translatable("block.nuclearpowered.cs_column"));
    }

    @Override public RecipeType<ReprocessingRecipe> getRecipeType() { return RECIPE_TYPE; }
}
