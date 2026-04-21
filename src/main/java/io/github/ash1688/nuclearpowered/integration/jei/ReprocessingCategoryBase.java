package io.github.ash1688.nuclearpowered.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Shared layout + render code for all six reprocessing-chain JEI categories.
// Inputs on the left, outputs on the right, fluid slot (if any) between them,
// FE cost + timing text below.
public abstract class ReprocessingCategoryBase implements IRecipeCategory<ReprocessingRecipe> {
    protected static final int WIDTH = 150;
    protected static final int HEIGHT = 60;

    private static final int INPUT_X = 4;
    private static final int INPUT_Y = 4;
    private static final int FLUID_X = 68;
    private static final int FLUID_Y = 4;
    private static final int OUTPUT_X = 100;
    private static final int OUTPUT_Y = 4;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    protected ReprocessingCategoryBase(IGuiHelper helper, ItemStack iconStack, Component title) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(iconStack);
        this.title = title;
    }

    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReprocessingRecipe recipe, IFocusGroup focuses) {
        // Stack inputs vertically on the left.
        for (int i = 0; i < recipe.inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y + i * 18)
                    .addItemStack(recipe.inputs.get(i));
        }
        if (!recipe.fluidInput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, FLUID_Y)
                    .addFluidStack(recipe.fluidInput.getFluid(), recipe.fluidInput.getAmount());
        }
        // Stack outputs vertically on the right.
        for (int i = 0; i < recipe.outputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y + i * 18)
                    .addItemStack(recipe.outputs.get(i));
        }
    }

    @Override
    public void draw(ReprocessingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView view,
                     GuiGraphics g, double mouseX, double mouseY) {
        // Arrow between input column and output column
        int arrowColor = 0xFF5A5A5A;
        g.fill(24, 10, 96, 13, arrowColor);
        g.fill(94, 8, 97, 15, arrowColor);
        g.fill(96, 9, 99, 14, arrowColor);

        Font font = Minecraft.getInstance().font;
        String fe = recipe.feCost + " FE";
        String time = String.format("%.1fs", recipe.processTicks / 20.0);
        g.drawString(font, fe, 4, 46, 0xFF333333, false);
        int timeW = font.width(time);
        g.drawString(font, time, WIDTH - timeW - 4, 46, 0xFF333333, false);
    }
}
