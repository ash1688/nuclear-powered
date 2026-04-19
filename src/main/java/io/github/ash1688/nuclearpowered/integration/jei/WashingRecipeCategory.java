package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.recipe.WasherRecipe;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class WashingRecipeCategory implements IRecipeCategory<WasherRecipe> {
    public static final RecipeType<WasherRecipe> RECIPE_TYPE =
            RecipeType.create(NuclearPowered.MODID, "washing", WasherRecipe.class);

    private static final int WIDTH = 138;
    private static final int HEIGHT = 46;

    private final IDrawable background;
    private final IDrawable icon;

    public WashingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(WIDTH, HEIGHT);
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.WASHER.get()));
    }

    @Override
    public RecipeType<WasherRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.nuclearpowered.washer");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WasherRecipe recipe, IFocusGroup focuses) {
        // Input item, fluid tank, output item.
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 15)
                .addIngredients(recipe.getInputIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 1)
                .setFluidRenderer(recipe.getFluid().getAmount(), false, 16, 32)
                .addIngredient(ForgeTypes.FLUID_STACK, recipe.getFluid());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 121, 15)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(WasherRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView view,
                     GuiGraphics g, double mouseX, double mouseY) {
        // Arrow between fluid tank and output slot.
        int color = 0xFF4A6AA5;
        g.fill(58, 20, 108, 23, color);
        g.fill(106, 18, 109, 25, color);
        g.fill(108, 19, 111, 24, color);
        g.fill(110, 20, 113, 23, color);

        Font font = Minecraft.getInstance().font;
        String time = String.format("%.1fs", recipe.getProcessingTime() / 20.0);
        int w = font.width(time);
        g.drawString(font, time, (WIDTH - w) / 2, 36, 0xFF333333, false);

        String mb = recipe.getFluid().getAmount() + " mB";
        int mw = font.width(mb);
        g.drawString(font, mb, 35 - mw / 2, 36, 0xFF2060D0, false);
    }
}
