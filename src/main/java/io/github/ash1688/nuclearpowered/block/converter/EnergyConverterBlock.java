package io.github.ash1688.nuclearpowered.block.converter;

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
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * FE &lt;-&gt; EU Converter. Acts as an omnidirectional bridge between Forge
 * energy (FE) networks and GT CEu's EU network; exposes both capabilities on
 * every face when GT is loaded, Forge energy only otherwise.
 *
 * <p>GUI is deferred to Phase E (LDLib reskin). Right-click currently shows a
 * plain-text readout of stored energy.</p>
 */
public class EnergyConverterBlock extends BaseEntityBlock {
    public EnergyConverterBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.ENERGY_CONVERTER.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyConverterBlockEntity converter) {
                // Temporary status readout until Phase E lands a real LDLib UI.
                int fe = converter.getStoredFE();
                int cap = converter.getCapacityFE();
                int eu = fe / EnergyConverterBlockEntity.FE_PER_EU;
                int euCap = cap / EnergyConverterBlockEntity.FE_PER_EU;
                player.displayClientMessage(Component.literal(
                        "Converter: " + fe + " / " + cap + " FE  (" + eu + " / " + euCap + " EU)"),
                        true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
