package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

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

    // Steam is never rendered as a world fluid, but JEI (and any mod that iterates
    // fluid types) asks for its still/flowing sprite — returning null crashes the
    // texture atlas lookup, so we point at the vanilla water sprites as a stand-in.
    private static final ResourceLocation WATER_STILL = new ResourceLocation("block/water_still");
    private static final ResourceLocation WATER_FLOW = new ResourceLocation("block/water_flow");

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
                    .canConvertToSource(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
                        @Override public int getTintColor() { return 0xFFCCCCCC; }
                    });
                }
            });

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

    // --- Tier 1 reprocessing reagents ---
    public static final RegistryObject<FluidType> NITRIC_ACID_TYPE = FLUID_TYPES.register("nitric_acid",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1500).viscosity(1000).temperature(300)
                    .canPushEntity(false).canSwim(false).canDrown(false)
                    .canExtinguish(true).canConvertToSource(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
                        @Override public int getTintColor() { return 0xFFE0D060; }
                    });
                }
            });

    public static final RegistryObject<ForgeFlowingFluid.Source> NITRIC_ACID =
            FLUIDS.register("nitric_acid", () -> new ForgeFlowingFluid.Source(nitricProperties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> NITRIC_ACID_FLOWING =
            FLUIDS.register("flowing_nitric_acid", () -> new ForgeFlowingFluid.Flowing(nitricProperties()));

    private static ForgeFlowingFluid.Properties nitricProperties() {
        return new ForgeFlowingFluid.Properties(NITRIC_ACID_TYPE, NITRIC_ACID, NITRIC_ACID_FLOWING)
                .slopeFindDistance(2).levelDecreasePerBlock(2)
                .bucket(ModItems.NITRIC_ACID_BUCKET);
    }

    public static final RegistryObject<FluidType> EXTRACTION_SOLVENT_TYPE = FLUID_TYPES.register("extraction_solvent",
            () -> new FluidType(FluidType.Properties.create()
                    .density(800).viscosity(1000).temperature(290)
                    .canPushEntity(false).canSwim(false).canDrown(false)
                    .canExtinguish(false).canConvertToSource(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
                        @Override public int getTintColor() { return 0xFFA06020; }
                    });
                }
            });

    public static final RegistryObject<ForgeFlowingFluid.Source> EXTRACTION_SOLVENT =
            FLUIDS.register("extraction_solvent", () -> new ForgeFlowingFluid.Source(extractionProperties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> EXTRACTION_SOLVENT_FLOWING =
            FLUIDS.register("flowing_extraction_solvent", () -> new ForgeFlowingFluid.Flowing(extractionProperties()));

    private static ForgeFlowingFluid.Properties extractionProperties() {
        return new ForgeFlowingFluid.Properties(EXTRACTION_SOLVENT_TYPE, EXTRACTION_SOLVENT, EXTRACTION_SOLVENT_FLOWING)
                .slopeFindDistance(2).levelDecreasePerBlock(2)
                .bucket(ModItems.EXTRACTION_SOLVENT_BUCKET);
    }

    private ModFluids() {}

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
        FLUID_TYPES.register(eventBus);
    }
}
