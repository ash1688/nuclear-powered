package io.github.ash1688.nuclearpowered.block.cable;

import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;

// EU-only cable. Visually mirrors the FE cable's multipart connection
// rendering (centre core + arms toward connected neighbours) but only
// connects to other EU cables and to neighbours exposing GT's
// IEnergyContainer cap. FE blocks are refused; FE cables are refused. The
// two networks live side-by-side and never bridge.
public class EnergyCableEUBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH = Block.box(5, 5,  0, 11, 11,  5);
    private static final VoxelShape ARM_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_WEST  = Block.box( 0, 5, 5,  5, 11, 11);
    private static final VoxelShape ARM_UP    = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_DOWN  = Block.box(5,  0, 5, 11,  5, 11);

    public EnergyCableEUBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(UP))    shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, ARM_DOWN);
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Direction dir : Direction.values()) {
            s = s.setValue(propertyFor(dir),
                    canConnect(ctx.getLevel(), ctx.getClickedPos(), dir));
        }
        return s;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        return state.setValue(propertyFor(facing), canConnect(level, pos, facing));
    }

    public static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST  -> EAST;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    /**
     * EU cable: connect to another EU cable, or any neighbour that exposes
     * GT's IEnergyContainer AND does NOT also expose Forge ENERGY on the
     * touching face. The Forge-ENERGY exclusion filters out FE-native blocks
     * that GTCEU has auto-shimmed with a GT cap — those are real FE
     * endpoints, not EU endpoints, and connecting to them would silently
     * void the energy through GT's compat layer.
     */
    public static boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos npos = pos.relative(dir);
        BlockState nstate = level.getBlockState(npos);
        if (nstate.getBlock() instanceof EnergyCableEUBlock) return true;
        BlockEntity be = level.getBlockEntity(npos);
        if (be == null) return false;
        if (!GTCompat.isLoaded()) return false;
        Direction face = dir.getOpposite();
        // Refuse FE-native blocks that GT auto-shimmed — they answer the GT
        // cap query but the underlying block is FE-only.
        if (be.getCapability(ForgeCapabilities.ENERGY, face).isPresent()) return false;
        return GTEnergyCompat.hasEUCapability(be, face);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableEUBlockEntity(pos, state);
    }
}
