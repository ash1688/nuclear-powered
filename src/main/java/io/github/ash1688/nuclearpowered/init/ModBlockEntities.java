package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.block.battery.BatteryBlockEntity;
import io.github.ash1688.nuclearpowered.block.boiler.CoalBoilerBlockEntity;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableBlockEntity;
import io.github.ash1688.nuclearpowered.block.crusher.CrusherBlockEntity;
import io.github.ash1688.nuclearpowered.block.cscolumn.CsColumnBlockEntity;
import io.github.ash1688.nuclearpowered.block.dissolver.DissolverBlockEntity;
import io.github.ash1688.nuclearpowered.block.electricfurnace.ElectricFurnaceBlockEntity;
import io.github.ash1688.nuclearpowered.block.extractor.ExtractionColumnBlockEntity;
import io.github.ash1688.nuclearpowered.block.engine.SteamEngineBlockEntity;
import io.github.ash1688.nuclearpowered.block.fabricator.FuelFabricatorBlockEntity;
import io.github.ash1688.nuclearpowered.block.heater.HeaterBlockEntity;
import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.block.pipe.SteamPipeBlockEntity;
import io.github.ash1688.nuclearpowered.block.shearer.ShearerBlockEntity;
import io.github.ash1688.nuclearpowered.block.thermocouple.ThermocoupleBlockEntity;
import io.github.ash1688.nuclearpowered.block.washer.WasherBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NuclearPowered.MODID);

    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () ->
                    BlockEntityType.Builder.of(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<WasherBlockEntity>> WASHER =
            BLOCK_ENTITIES.register("washer", () ->
                    BlockEntityType.Builder.of(WasherBlockEntity::new, ModBlocks.WASHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () ->
                    BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PileBlockEntity>> GRAPHITE_PILE =
            BLOCK_ENTITIES.register("graphite_pile", () ->
                    BlockEntityType.Builder.of(PileBlockEntity::new, ModBlocks.GRAPHITE_PILE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ThermocoupleBlockEntity>> THERMOCOUPLE =
            BLOCK_ENTITIES.register("thermocouple", () ->
                    BlockEntityType.Builder.of(ThermocoupleBlockEntity::new, ModBlocks.THERMOCOUPLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable", () ->
                    BlockEntityType.Builder.of(EnergyCableBlockEntity::new, ModBlocks.ENERGY_CABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITIES.register("battery", () ->
                    BlockEntityType.Builder.of(BatteryBlockEntity::new, ModBlocks.BATTERY.get()).build(null));

    public static final RegistryObject<BlockEntityType<FuelFabricatorBlockEntity>> FUEL_FABRICATOR =
            BLOCK_ENTITIES.register("fuel_fabricator", () ->
                    BlockEntityType.Builder.of(FuelFabricatorBlockEntity::new, ModBlocks.FUEL_FABRICATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<CoalBoilerBlockEntity>> COAL_BOILER =
            BLOCK_ENTITIES.register("coal_boiler", () ->
                    BlockEntityType.Builder.of(CoalBoilerBlockEntity::new, ModBlocks.COAL_BOILER.get()).build(null));

    public static final RegistryObject<BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
            BLOCK_ENTITIES.register("steam_engine", () ->
                    BlockEntityType.Builder.of(SteamEngineBlockEntity::new, ModBlocks.STEAM_ENGINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SteamPipeBlockEntity>> STEAM_PIPE =
            BLOCK_ENTITIES.register("steam_pipe", () ->
                    BlockEntityType.Builder.of(SteamPipeBlockEntity::new, ModBlocks.STEAM_PIPE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShearerBlockEntity>> SHEARER =
            BLOCK_ENTITIES.register("shearer", () ->
                    BlockEntityType.Builder.of(ShearerBlockEntity::new, ModBlocks.SHEARER.get()).build(null));

    public static final RegistryObject<BlockEntityType<DissolverBlockEntity>> DISSOLVER =
            BLOCK_ENTITIES.register("dissolver", () ->
                    BlockEntityType.Builder.of(DissolverBlockEntity::new, ModBlocks.DISSOLVER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ExtractionColumnBlockEntity>> EXTRACTION_COLUMN =
            BLOCK_ENTITIES.register("extraction_column", () ->
                    BlockEntityType.Builder.of(ExtractionColumnBlockEntity::new, ModBlocks.EXTRACTION_COLUMN.get()).build(null));

    public static final RegistryObject<BlockEntityType<CsColumnBlockEntity>> CS_COLUMN =
            BLOCK_ENTITIES.register("cs_column", () ->
                    BlockEntityType.Builder.of(CsColumnBlockEntity::new, ModBlocks.CS_COLUMN.get()).build(null));

    public static final RegistryObject<BlockEntityType<HeaterBlockEntity>> HEATER =
            BLOCK_ENTITIES.register("heater", () ->
                    BlockEntityType.Builder.of(HeaterBlockEntity::new, ModBlocks.HEATER.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
