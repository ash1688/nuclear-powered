package io.github.ash1688.nuclearpowered.block.thermocouple;

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
import io.github.ash1688.nuclearpowered.block.FacingMachineBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;

import javax.annotation.Nullable;

public class ThermocoupleBlock extends FacingMachineBlock {
    public ThermocoupleBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermocoupleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.THERMOCOUPLE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ThermocoupleBlockEntity tc) {
                ItemStack held = player.getItemInHand(hand);
                // Heat Capture Efficiency Core upgrade — consumed on first apply,
                // noop thereafter. Checked before the coolant-mode sneak toggle so
                // the upgrade lands regardless of the player's crouch state.
                if (held.is(ModItems.HEAT_CAPTURE_EFFICIENCY_CORE.get())) {
                    boolean fresh = tc.applyHeatCaptureEfficiency();
                    player.displayClientMessage(
                            Component.literal(fresh
                                    ? "Heat Capture Efficiency Core installed"
                                    : "Thermocouple already has Heat Capture Efficiency Core"),
                            true);
                    if (fresh) {
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
                    }
                    return InteractionResult.CONSUME;
                }
                // Sneak + right-click toggles coolant mode (heat dumped to pile,
                // no FE pushed out). Regular right-click opens the GUI.
                if (player.isShiftKeyDown()) {
                    tc.toggleCoolantMode();
                    player.displayClientMessage(
                            Component.literal("Thermocouple: " + (tc.isCoolantMode() ? "COOLANT MODE" : "NORMAL")),
                            true);
                } else if (player instanceof ServerPlayer sp) {
                    BlockEntityUIFactory.INSTANCE.openUI(tc, sp);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
