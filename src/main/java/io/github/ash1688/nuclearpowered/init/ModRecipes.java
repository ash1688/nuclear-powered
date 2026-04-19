package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.recipe.CrusherRecipe;
import io.github.ash1688.nuclearpowered.recipe.FuelFabricatorRecipe;
import io.github.ash1688.nuclearpowered.recipe.WasherRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, NuclearPowered.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, NuclearPowered.MODID);

    public static final RegistryObject<RecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
            SERIALIZERS.register("crushing", () -> CrusherRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<CrusherRecipe>> CRUSHING_TYPE =
            TYPES.register("crushing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "nuclearpowered:crushing";
                }
            });

    public static final RegistryObject<RecipeSerializer<WasherRecipe>> WASHING_SERIALIZER =
            SERIALIZERS.register("washing", () -> WasherRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<WasherRecipe>> WASHING_TYPE =
            TYPES.register("washing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "nuclearpowered:washing";
                }
            });

    public static final RegistryObject<RecipeSerializer<FuelFabricatorRecipe>> FABRICATING_SERIALIZER =
            SERIALIZERS.register("fabricating", () -> FuelFabricatorRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<FuelFabricatorRecipe>> FABRICATING_TYPE =
            TYPES.register("fabricating", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "nuclearpowered:fabricating";
                }
            });

    private ModRecipes() {}

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
