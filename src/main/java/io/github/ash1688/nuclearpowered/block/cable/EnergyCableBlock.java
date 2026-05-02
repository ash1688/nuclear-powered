package io.github.ash1688.nuclearpowered.block.cable;

import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.energy.EnergyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

// Cables don't tick — energy moves through receiveEnergy push as soon as a
// producer puts power into the network. The block carries 6 boolean properties
// (one per face) so the multipart blockstate can render arms only on faces
// that actually have a neighbour worth connecting to: another cable, or any
// block that exposes Forge ENERGY.
public class EnergyCableBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    // 6 px-thick centre core (5..11 along each axis) so it visually reads
    // as a wire rather than a full cube even when no arms are connected.
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH = Block.box(5, 5,  0, 11, 11,  5);
    private static final VoxelShape ARM_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_WEST  = Block.box( 0, 5, 5,  5, 11, 11);
    private static final VoxelShape ARM_UP    = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_DOWN  = Block.box(5,  0, 5, 11,  5, 11);

    public EnergyCableBlock(Properties props) {
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
        // Recompute mode in case a connecting/disconnecting neighbour shifts
        // the cluster's energy system (e.g. removing the only FE producer
        // leaves only EU producers, so the cable should swap to EU).
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyCableBlockEntity cable) cable.recomputeMode();
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
     * Connect to a neighbour if its energy system matches this cable's mode.
     *
     * <ul>
     *   <li>Cable→cable: connect if both cables share the same mode. Lets
     *       a homogeneous run (all FE or all EU) line up cleanly while
     *       still refusing where two networks cross modes.</li>
     *   <li>Cable→other: connect to a neighbour exposing the cable's
     *       active capability — Forge ENERGY for FE-mode, GT
     *       IEnergyContainer for EU-mode.</li>
     * </ul>
     */
    public static boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos npos = pos.relative(dir);
        BlockState nstate = level.getBlockState(npos);
        BlockEntity selfBE = level.getBlockEntity(pos);
        EnergyMode selfMode = (selfBE instanceof EnergyCableBlockEntity sc) ? sc.getMode() : EnergyMode.FE;

        if (nstate.getBlock() instanceof EnergyCableBlock) {
            BlockEntity nbe = level.getBlockEntity(npos);
            EnergyMode nMode = (nbe instanceof EnergyCableBlockEntity nc) ? nc.getMode() : EnergyMode.FE;
            return nMode == selfMode;
        }

        BlockEntity be = level.getBlockEntity(npos);
        if (be == null) return false;
        if (selfMode == EnergyMode.FE) {
            return be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent();
        }
        // EU mode — connect via the GT cap. Guarded by GTCompat so absent-GT
        // packs simply never see EU connections.
        return GTCompat.isLoaded()
                && GTEnergyCompat.hasEUCapability(be, dir.getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }
}
