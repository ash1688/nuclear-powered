package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.block.battery.BatteryBlockEntity;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableBlockEntity;
import io.github.ash1688.nuclearpowered.block.crusher.CrusherBlockEntity;
import io.github.ash1688.nuclearpowered.block.electricfurnace.ElectricFurnaceBlockEntity;
import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
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

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
