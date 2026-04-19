package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Minimal Forge fluid registration for steam. Steam is used inside boilers and engines
 * via FluidTank + FluidStack — never placed as a world block, so no bucket item, no
 * LiquidBlock, and no client-side fluid texture is necessary for MVP. GUI rendering
 * draws the tank as a solid colour via GuiGraphics.fill().
 */
public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, NuclearPowered.MODID);

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NuclearPowered.MODID);

    public static final ResourceLocation MISSING_TEXTURE = new ResourceLocation("minecraft", "block/water_still");

    public static final RegistryObject<FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create()
                    .density(-1000)          // negative = gaseous (floats up in a fluid world)
                    .viscosity(200)
                    .temperature(373)         // ~100°C in kelvin, close to real steam
                    .lightLevel(0)
                    .canPushEntity(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)));

    public static final RegistryObject<ForgeFlowingFluid.Source> STEAM =
            FLUIDS.register("steam",
                    () -> new ForgeFlowingFluid.Source(steamProperties()));

    public static final RegistryObject<ForgeFlowingFluid.Flowing> STEAM_FLOWING =
            FLUIDS.register("flowing_steam",
                    () -> new ForgeFlowingFluid.Flowing(steamProperties()));

    private static ForgeFlowingFluid.Properties steamProperties() {
        return new ForgeFlowingFluid.Properties(STEAM_TYPE, STEAM, STEAM_FLOWING)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    private ModFluids() {}

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
        FLUID_TYPES.register(eventBus);
    }
}
