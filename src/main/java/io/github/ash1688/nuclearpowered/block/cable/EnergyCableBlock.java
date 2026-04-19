package io.github.ash1688.nuclearpowered.block.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// Cables no longer tick — the network-based conduit delivers FE synchronously
// when a producer pushes into its receiveEnergy, so no per-tick work is needed.
public class EnergyCableBlock extends BaseEntityBlock {
    public EnergyCableBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }
}
