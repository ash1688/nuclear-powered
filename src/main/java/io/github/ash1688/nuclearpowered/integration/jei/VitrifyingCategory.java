package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class VitrifyingCategory extends ReprocessingCategoryBase {
    public static final RecipeType<ReprocessingRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(NuclearPowered.MODID, "vitrifying"), ReprocessingRecipe.class);

    public VitrifyingCategory(IGuiHelper helper) {
        super(helper, new ItemStack(ModBlocks.VITRIFIER.get()),
                Component.translatable("block.nuclearpowered.vitrifier"));
    }

    @Override public RecipeType<ReprocessingRecipe> getRecipeType() { return RECIPE_TYPE; }
}
