package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
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
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RecipeManager rm = level.getRecipeManager();
        registration.addRecipes(CrushingRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get()));
        registration.addRecipes(WashingRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipes.WASHING_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()),
                CrushingRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WASHER.get()),
                WashingRecipeCategory.RECIPE_TYPE);
        // Electric furnace processes vanilla SMELTING recipes, so it's a catalyst for the
        // built-in smelting category — clicking the furnace in JEI shows every smelting recipe.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()),
                RecipeTypes.SMELTING);
    }
}
