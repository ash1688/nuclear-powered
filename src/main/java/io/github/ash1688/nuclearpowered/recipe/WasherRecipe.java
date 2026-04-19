package io.github.ash1688.nuclearpowered.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public class WasherRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 60;

    private final ResourceLocation id;
    private final Ingredient input;
    private final FluidStack fluid;
    private final ItemStack output;
    private final int processingTime;

    public WasherRecipe(ResourceLocation id, Ingredient input, FluidStack fluid,
                        ItemStack output, int processingTime) {
        this.id = id;
        this.input = input;
        this.fluid = fluid;
        this.output = output;
        this.processingTime = processingTime;
    }

    public Ingredient getInputIngredient() { return input; }
    public FluidStack getFluid() { return fluid; }
    public ItemStack getResult() { return output; }
    public int getProcessingTime() { return processingTime; }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide) return false;
        return input.test(container.getItem(0));
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
    public RecipeSerializer<?> getSerializer() { return ModRecipes.WASHING_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipes.WASHING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<WasherRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(NuclearPowered.MODID, "washing");

        @Override
        public WasherRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            FluidStack fluid = fluidFromJson(GsonHelper.getAsJsonObject(json, "fluid"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int time = GsonHelper.getAsInt(json, "processing_time", DEFAULT_PROCESSING_TIME);
            return new WasherRecipe(id, input, fluid, output, time);
        }

        @Override
        public WasherRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            FluidStack fluid = buf.readFluidStack();
            ItemStack output = buf.readItem();
            int time = buf.readVarInt();
            return new WasherRecipe(id, input, fluid, output, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, WasherRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeFluidStack(recipe.fluid);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.processingTime);
        }

        private static FluidStack fluidFromJson(JsonObject json) {
            String id = GsonHelper.getAsString(json, "fluid");
            int amount = GsonHelper.getAsInt(json, "amount");
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id));
            if (fluid == null) {
                throw new JsonSyntaxException("Unknown fluid '" + id + "' in washing recipe");
            }
            return new FluidStack(fluid, amount);
        }
    }
}
