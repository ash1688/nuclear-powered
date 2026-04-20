package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.block.battery.BatteryBlock;
import io.github.ash1688.nuclearpowered.block.boiler.CoalBoilerBlock;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableBlock;
import io.github.ash1688.nuclearpowered.block.crusher.CrusherBlock;
import io.github.ash1688.nuclearpowered.block.cscolumn.CsColumnBlock;
import io.github.ash1688.nuclearpowered.block.dissolver.DissolverBlock;
import io.github.ash1688.nuclearpowered.block.electricfurnace.ElectricFurnaceBlock;
import io.github.ash1688.nuclearpowered.block.extractor.ExtractionColumnBlock;
import io.github.ash1688.nuclearpowered.block.engine.SteamEngineBlock;
import io.github.ash1688.nuclearpowered.block.fabricator.FuelFabricatorBlock;
import io.github.ash1688.nuclearpowered.block.heater.HeaterBlock;
import io.github.ash1688.nuclearpowered.block.pile.PileBlock;
import io.github.ash1688.nuclearpowered.block.pipe.SteamPipeBlock;
import io.github.ash1688.nuclearpowered.block.recycler.CladdingRecyclerBlock;
import io.github.ash1688.nuclearpowered.block.shearer.ShearerBlock;
import io.github.ash1688.nuclearpowered.block.thermocouple.ThermocoupleBlock;
import io.github.ash1688.nuclearpowered.block.vitrifier.VitrifierBlock;
import io.github.ash1688.nuclearpowered.block.washer.WasherBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NuclearPowered.MODID);

    public static final RegistryObject<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NETHER_URANIUM_ORE = registerBlock("nether_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.NETHER_ORE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> END_URANIUM_ORE = registerBlock("end_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_THORIUM_ORE = registerBlock("deepslate_thorium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NETHER_THORIUM_ORE = registerBlock("nether_thorium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.NETHER_ORE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> END_THORIUM_ORE = registerBlock("end_thorium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<CrusherBlock> CRUSHER = registerBlock("crusher",
            () -> new CrusherBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<WasherBlock> WASHER = registerBlock("washer",
            () -> new WasherBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ElectricFurnaceBlock> ELECTRIC_FURNACE = registerBlock("electric_furnace",
            () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)));

    public static final RegistryObject<PileBlock> GRAPHITE_PILE = registerBlock("graphite_pile",
            () -> new PileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0f, 5.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // Plain graphite brick used as pile casing. Detected by the pile at runtime to
    // expand the structure's heat capacity — no BlockEntity, purely decorative.
    public static final RegistryObject<Block> GRAPHITE_CASING = registerBlock("graphite_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<ThermocoupleBlock> THERMOCOUPLE = registerBlock("thermocouple",
            () -> new ThermocoupleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 5)));

    public static final RegistryObject<EnergyCableBlock> ENERGY_CABLE = registerBlock("energy_cable",
            () -> new EnergyCableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f, 2.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<BatteryBlock> BATTERY = registerBlock("battery",
            () -> new BatteryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f, 4.5f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<FuelFabricatorBlock> FUEL_FABRICATOR = registerBlock("fuel_fabricator",
            () -> new FuelFabricatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<CoalBoilerBlock> COAL_BOILER = registerBlock("coal_boiler",
            () -> new CoalBoilerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 8)));

    public static final RegistryObject<SteamEngineBlock> STEAM_ENGINE = registerBlock("steam_engine",
            () -> new SteamEngineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ShearerBlock> SHEARER = registerBlock("shearer",
            () -> new ShearerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<DissolverBlock> DISSOLVER = registerBlock("dissolver",
            () -> new DissolverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ExtractionColumnBlock> EXTRACTION_COLUMN = registerBlock("extraction_column",
            () -> new ExtractionColumnBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<CsColumnBlock> CS_COLUMN = registerBlock("cs_column",
            () -> new CsColumnBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<VitrifierBlock> VITRIFIER = registerBlock("vitrifier",
            () -> new VitrifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<CladdingRecyclerBlock> CLADDING_RECYCLER = registerBlock("cladding_recycler",
            () -> new CladdingRecyclerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<HeaterBlock> HEATER = registerBlock("heater",
            () -> new HeaterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(HeaterBlock.ACTIVE) ? 10 : 0)));

    public static final RegistryObject<SteamPipeBlock> STEAM_PIPE = registerBlock("steam_pipe",
            () -> new SteamPipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 2.0f)
                    .sound(SoundType.METAL)));

    private ModBlocks() {}

    // Registers a block and its corresponding BlockItem in one call.
    // The BlockItem uses default properties; override-needing blocks
    // will want a separate registerBlock variant when that day comes.
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
