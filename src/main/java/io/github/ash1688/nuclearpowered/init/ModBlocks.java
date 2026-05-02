package io.github.ash1688.nuclearpowered.init;

import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.block.battery.BatteryBlock;
import io.github.ash1688.nuclearpowered.block.boiler.CoalBoilerBlock;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableBlock;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableEUBlock;
import io.github.ash1688.nuclearpowered.block.converter.EnergyConverterBlock;
import io.github.ash1688.nuclearpowered.block.coolingpond.CoolingPondBlock;
import io.github.ash1688.nuclearpowered.block.crusher.CrusherBlock;
import io.github.ash1688.nuclearpowered.block.cscolumn.CsColumnBlock;
import io.github.ash1688.nuclearpowered.block.dissolver.DissolverBlock;
import io.github.ash1688.nuclearpowered.block.electricfurnace.ElectricFurnaceBlock;
import io.github.ash1688.nuclearpowered.block.extractor.ExtractionColumnBlock;
import io.github.ash1688.nuclearpowered.block.FacingPlaceableBlock;
import io.github.ash1688.nuclearpowered.block.creative.CreativeEUGeneratorBlock;
import io.github.ash1688.nuclearpowered.block.creative.CreativeFEGeneratorBlock;
import io.github.ash1688.nuclearpowered.block.engine.SteamEngineBlock;
import io.github.ash1688.nuclearpowered.block.fabricator.FuelFabricatorBlock;
import io.github.ash1688.nuclearpowered.block.heater.HeaterBlock;
import io.github.ash1688.nuclearpowered.block.pile.PileBlock;
import io.github.ash1688.nuclearpowered.block.pipe.SteamPipeBlock;
import io.github.ash1688.nuclearpowered.block.recycler.CladdingRecyclerBlock;
import io.github.ash1688.nuclearpowered.block.shearer.ShearerBlock;
import io.github.ash1688.nuclearpowered.block.thermocouple.ThermocoupleBlock;
import io.github.ash1688.nuclearpowered.block.vitrifier.VitrifierBlock;
import io.github.ash1688.nuclearpowered.block.watersource.InfiniteNitricAcidSourceBlock;
import io.github.ash1688.nuclearpowered.block.watersource.InfiniteSolventSourceBlock;
import io.github.ash1688.nuclearpowered.block.watersource.InfiniteWaterSourceBlock;
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

    public static final RegistryObject<PileBlock> GRAPHITE_PILE_CONTROLLER = registerBlock("graphite_pile_controller",
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

    // Stand-in casing block that pulls depleted rods out of the pile and pushes
    // them into an adjacent item handler (Cooling Pond, chest, hopper). Allowed
    // in any of the 8 outer bottom-row positions of a 3x3x3 shell; the pile's
    // structure check rejects it elsewhere.
    public static final RegistryObject<io.github.ash1688.nuclearpowered.block.pile.output.FuelRodOutputPortBlock>
            FUEL_ROD_OUTPUT_PORT = registerBlock("fuel_rod_output_port",
                    () -> new io.github.ash1688.nuclearpowered.block.pile.output.FuelRodOutputPortBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(3.5f, 4.0f)
                                    .sound(SoundType.METAL)
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

    // EU-only cable. Lives alongside the FE cable as a parallel network —
    // GT producers / consumers connect, FE blocks are refused.
    public static final RegistryObject<EnergyCableEUBlock> ENERGY_CABLE_EU = registerBlock("energy_cable_eu",
            () -> new EnergyCableEUBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
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

    public static final RegistryObject<CoolingPondBlock> COOLING_POND = registerBlock("cooling_pond",
            () -> new CoolingPondBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.STONE)));

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

    // Creative / test helpers — no recipes, grab from creative tab.
    public static final RegistryObject<InfiniteWaterSourceBlock> INFINITE_WATER_SOURCE = registerBlock("infinite_water_source",
            () -> new InfiniteWaterSourceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.GLASS)));

    public static final RegistryObject<InfiniteNitricAcidSourceBlock> INFINITE_NITRIC_ACID_SOURCE = registerBlock("infinite_nitric_acid_source",
            () -> new InfiniteNitricAcidSourceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.GLASS)));

    public static final RegistryObject<InfiniteSolventSourceBlock> INFINITE_SOLVENT_SOURCE = registerBlock("infinite_solvent_source",
            () -> new InfiniteSolventSourceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.GLASS)));

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

    public static final RegistryObject<EnergyConverterBlock> ENERGY_CONVERTER = registerBlock("energy_converter",
            () -> new EnergyConverterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)));

    // --- Component blocks (no BlockEntity logic yet — placeholders) ---
    // Tank/vat are storage components; fluid_pipe transports fluids. All three
    // place as plain blocks for now and gain BE behavior in a follow-up.
    public static final RegistryObject<Block> TANK = registerBlock("tank",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> FLUID_PIPE = registerBlock("fluid_pipe",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 2.0f)
                    .sound(SoundType.METAL)));

    // --- New processing machines (no BlockEntity logic yet — placeholders) ---
    // Stamping press, macerator, slicer, wiremill, rubber squeezer. Place
    // facing the player; recipe-driven processing wires up later.
    public static final RegistryObject<FacingPlaceableBlock> STAMPING_PRESS = registerBlock("stamping_press",
            () -> new FacingPlaceableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<FacingPlaceableBlock> MACERATOR = registerBlock("macerator",
            () -> new FacingPlaceableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<FacingPlaceableBlock> SLICER = registerBlock("slicer",
            () -> new FacingPlaceableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<FacingPlaceableBlock> WIREMILL = registerBlock("wiremill",
            () -> new FacingPlaceableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<FacingPlaceableBlock> RUBBER_SQUEEZER = registerBlock("rubber_squeezer",
            () -> new FacingPlaceableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 4.0f)
                    .sound(SoundType.METAL)));

    // --- Creative test sources for the dual-energy system ---
    // Infinite FE / EU emitters; place adjacent to a machine to flood it
    // with power. Useful for verifying mode-toggle + cable connection
    // gating without building the full energy chain.
    public static final RegistryObject<CreativeFEGeneratorBlock> CREATIVE_FE_GENERATOR = registerBlock("creative_fe_generator",
            () -> new CreativeFEGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<CreativeEUGeneratorBlock> CREATIVE_EU_GENERATOR = registerBlock("creative_eu_generator",
            () -> new CreativeEUGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0f, 3600000.0f)
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
