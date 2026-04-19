package io.github.ash1688.nuclearpowered.recipe;

import com.google.gson.JsonObject;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class FuelFabricatorRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 200;

    private final ResourceLocation id;
    private final Ingredient fuelIngredient;
    private final int fuelCount;
    private final Ingredient claddingIngredient;
    private final int claddingCount;
    private final ItemStack output;
    private final int processingTime;

    public FuelFabricatorRecipe(ResourceLocation id, Ingredient fuelIngredient, int fuelCount,
                                Ingredient claddingIngredient, int claddingCount,
                                ItemStack output, int processingTime) {
        this.id = id;
        this.fuelIngredient = fuelIngredient;
        this.fuelCount = fuelCount;
        this.claddingIngredient = claddingIngredient;
        this.claddingCount = claddingCount;
        this.output = output;
        this.processingTime = processingTime;
    }

    public Ingredient getFuelIngredient() { return fuelIngredient; }
    public int getFuelCount() { return fuelCount; }
    public Ingredient getCladdingIngredient() { return claddingIngredient; }
    public int getCladdingCount() { return claddingCount; }
    public ItemStack getResult() { return output; }
    public int getProcessingTime() { return processingTime; }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide) return false;
        ItemStack fuel = container.getItem(0);
        ItemStack cladding = container.getItem(1);
        return fuelIngredient.test(fuel) && fuel.getCount() >= fuelCount
                && claddingIngredient.test(cladding) && cladding.getCount() >= claddingCount;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) { return output.copy(); }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.FABRICATING_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipes.FABRICATING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<FuelFabricatorRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(NuclearPowered.MODID, "fabricating");

        @Override
        public FuelFabricatorRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient fuel = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "fuel_ingredient"));
            int fuelCount = GsonHelper.getAsInt(json, "fuel_count", 1);
            Ingredient cladding = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "cladding_ingredient"));
            int claddingCount = GsonHelper.getAsInt(json, "cladding_count", 1);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int time = GsonHelper.getAsInt(json, "processing_time", DEFAULT_PROCESSING_TIME);
            return new FuelFabricatorRecipe(id, fuel, fuelCount, cladding, claddingCount, result, time);
        }

        @Override
        public FuelFabricatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient fuel = Ingredient.fromNetwork(buf);
            int fuelCount = buf.readVarInt();
            Ingredient cladding = Ingredient.fromNetwork(buf);
            int claddingCount = buf.readVarInt();
            ItemStack result = buf.readItem();
            int time = buf.readVarInt();
            return new FuelFabricatorRecipe(id, fuel, fuelCount, cladding, claddingCount, result, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, FuelFabricatorRecipe recipe) {
            recipe.fuelIngredient.toNetwork(buf);
            buf.writeVarInt(recipe.fuelCount);
            recipe.claddingIngredient.toNetwork(buf);
            buf.writeVarInt(recipe.claddingCount);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.processingTime);
        }
    }
}
