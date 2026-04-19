package io.github.ash1688.nuclearpowered.integration.jei;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.recipe.FuelFabricatorRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

public class FabricatingRecipeCategory implements IRecipeCategory<FuelFabricatorRecipe> {
    public static final RecipeType<FuelFabricatorRecipe> RECIPE_TYPE =
            RecipeType.create(NuclearPowered.MODID, "fabricating", FuelFabricatorRecipe.class);

    private static final int WIDTH = 138;
    private static final int HEIGHT = 46;

    private final IDrawable background;
    private final IDrawable icon;

    public FabricatingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(WIDTH, HEIGHT);
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.FUEL_FABRICATOR.get()));
    }

    @Override
    public RecipeType<FuelFabricatorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.nuclearpowered.fuel_fabricator");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelFabricatorRecipe recipe, IFocusGroup focuses) {
        // Two input slots stacked vertically on the left, output slot on the right.
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5)
                .addIngredients(scale(recipe.getFuelIngredient(), recipe.getFuelCount()));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 23)
                .addIngredients(scale(recipe.getCladdingIngredient(), recipe.getCladdingCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 121, 14)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(FuelFabricatorRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView view,
                     GuiGraphics g, double mouseX, double mouseY) {
        // Arrow between input stack and output.
        int color = 0xFF5A5A5A;
        g.fill(30, 20, 108, 23, color);
        g.fill(106, 18, 109, 25, color);
        g.fill(108, 19, 111, 24, color);
        g.fill(110, 20, 113, 23, color);

        Font font = Minecraft.getInstance().font;
        String time = String.format("%.1fs", recipe.getProcessingTime() / 20.0);
        int w = font.width(time);
        g.drawString(font, time, (WIDTH - w) / 2, 36, 0xFF333333, false);
    }

    // Clones the ingredient's itemstacks and sets their count to the recipe's required count
    // so JEI shows the scaled amount next to each input slot rather than a single item.
    private static Ingredient scale(Ingredient ingredient, int count) {
        ItemStack[] stacks = ingredient.getItems();
        ItemStack[] scaled = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            scaled[i] = stacks[i].copy();
            scaled[i].setCount(count);
        }
        return Ingredient.of(scaled);
    }
}
