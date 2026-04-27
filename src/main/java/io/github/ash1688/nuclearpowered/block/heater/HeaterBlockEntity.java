package io.github.ash1688.nuclearpowered.block.heater;

import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Scans its six neighbours for a graphite pile and pumps heat into it while the
// block's ACTIVE blockstate is true. Boost rate ramps down linearly from
// BOOST_BELOW (full boost) to BOOST_STOPS_AT (zero boost) so it behaves like a
// real preheater — slams the cold pile up to operating range then backs off.
public class HeaterBlockEntity extends BlockEntity {
    private static final int BOOST_PER_TICK = 20;
    private static final int BOOST_BELOW = 1500;      // full rate at or under this heat
    private static final int BOOST_STOPS_AT = 2500;   // zero boost at or over this heat

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (!state.getValue(HeaterBlock.ACTIVE)) return;
        for (Direction dir : Direction.values()) {
            BlockPos npos = pos.relative(dir);
            if (!level.getBlockState(npos).is(ModBlocks.GRAPHITE_PILE_CONTROLLER.get())) continue;
            BlockEntity be = level.getBlockEntity(npos);
            if (!(be instanceof PileBlockEntity pile)) continue;
            int heat = pile.getHeat();
            if (heat >= BOOST_STOPS_AT) return;
            int boost;
            if (heat <= BOOST_BELOW) {
                boost = BOOST_PER_TICK;
            } else {
                // Linear ramp between BOOST_BELOW and BOOST_STOPS_AT.
                int span = BOOST_STOPS_AT - BOOST_BELOW;
                boost = Math.max(0, BOOST_PER_TICK * (BOOST_STOPS_AT - heat) / span);
            }
            if (boost > 0) pile.addHeat(boost);
            return;
        }
    }
}
