package io.github.ash1688.nuclearpowered.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// Steam pipe — like the energy cable, the block entity handles all transport
// work synchronously through its IFluidHandler pass-through, so no ticker needed.
public class SteamPipeBlock extends BaseEntityBlock {
    public SteamPipeBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamPipeBlockEntity(pos, state);
    }
}
