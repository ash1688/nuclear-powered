package io.github.ash1688.nuclearpowered.block.heater;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

// Right-clickable heater. Toggles its ACTIVE blockstate flag and — while active —
// pushes heat into an adjacent graphite pile using a ramp: full boost below
// BOOST_FULL_BELOW, tapering to zero at BOOST_STOPS_AT. Designed as a startup
// assist so players can warm a cold pile without waiting on fuel rods alone.
public class HeaterBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public HeaterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.HEATER.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean nowActive = !state.getValue(ACTIVE);
            level.setBlock(pos, state.setValue(ACTIVE, nowActive), 3);
            player.displayClientMessage(
                    Component.literal("Heater " + (nowActive ? "ON" : "OFF")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
