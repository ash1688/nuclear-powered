package io.github.ash1688.nuclearpowered.block.pile.output;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// Pile-shell port. Replaces one of the 8 outer bottom-row casings of a 3x3x3
// graphite shell; pulls hot spent rods out of the pile each tick and pushes
// them into whichever neighbour exposes an item handler (Cooling Pond, chest,
// hopper). The pile's existing structure check still passes when this block
// stands in for a casing — see PileBlockEntity.isShellAround.
public class FuelRodOutputPortBlock extends BaseEntityBlock {
    public FuelRodOutputPortBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelRodOutputPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.FUEL_ROD_OUTPUT_PORT.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level,
                         BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FuelRodOutputPortBlockEntity port) {
                port.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
