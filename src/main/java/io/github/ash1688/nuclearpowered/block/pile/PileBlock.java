package io.github.ash1688.nuclearpowered.block.pile;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class PileBlock extends BaseEntityBlock {
    public PileBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.GRAPHITE_PILE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PileBlockEntity pile) {
                ItemStack held = player.getItemInHand(hand);
                // Permanent upgrades — if the player is holding one, try to apply
                // it instead of opening the GUI. Consumes exactly one item on a
                // successful first-time apply; does nothing if already applied.
                if (held.is(ModItems.EXTENDED_BURN_MODULE.get())) {
                    return applyUpgrade(level, pos, player, held,
                            pile.applyExtendedBurn(), "Extended Burn Module installed",
                            "Pile already has Extended Burn Module");
                }
                if (held.is(ModItems.THERMAL_DAMPENER.get())) {
                    return applyUpgrade(level, pos, player, held,
                            pile.applyThermalDampener(), "Thermal Dampener installed",
                            "Pile already has Thermal Dampener");
                }
                if (player instanceof ServerPlayer sp) {
                    NetworkHooks.openScreen(sp, pile, buf -> buf.writeBlockPos(pos));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult applyUpgrade(Level level, BlockPos pos, Player player,
                                                   ItemStack held, boolean appliedFresh,
                                                   String successMsg, String alreadyMsg) {
        player.displayClientMessage(
                Component.literal(appliedFresh ? successMsg : alreadyMsg), true);
        if (appliedFresh) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PileBlockEntity pile) {
                pile.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
