package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.recipe.CrusherRecipe;
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

public class CrushingRecipeCategory implements IRecipeCategory<CrusherRecipe> {
    public static final RecipeType<CrusherRecipe> RECIPE_TYPE =
            RecipeType.create(NuclearPowered.MODID, "crushing", CrusherRecipe.class);

    private static final int WIDTH = 108;
    private static final int HEIGHT = 38;

    private final IDrawable background;
    private final IDrawable icon;

    public CrushingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(WIDTH, HEIGHT);
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CRUSHER.get()));
    }

    @Override
    public RecipeType<CrusherRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.nuclearpowered.crusher");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrusherRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 11)
                .addIngredients(recipe.getInputIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(CrusherRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView view,
                     GuiGraphics g, double mouseX, double mouseY) {
        // Simple horizontal arrow between slots (shaft + triangle tip).
        int color = 0xFF5A5A5A;
        g.fill(30, 17, 75, 20, color);
        g.fill(73, 15, 76, 22, color);
        g.fill(75, 16, 78, 21, color);
        g.fill(77, 17, 80, 20, color);

        Font font = Minecraft.getInstance().font;
        String time = String.format("%.1fs", recipe.getProcessingTime() / 20.0);
        int w = font.width(time);
        g.drawString(font, time, (WIDTH - w) / 2, 30, 0xFF333333, false);
    }
}
