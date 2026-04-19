package io.github.ash1688.nuclearpowered.block.crusher;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrusherBlock extends BaseEntityBlock {
    public CrusherBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE. Return MODEL so the block renders its JSON model.
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // GUI opening is wired in commit 2. For now, acknowledge the click so placement
        // behaviour matches the final crusher (right-click → machine interaction, not place).
        if (!level.isClientSide) {
            // TODO(commit 2): NetworkHooks.openScreen((ServerPlayer) player, crusherBE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrusherBlockEntity crusher) {
                crusher.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
