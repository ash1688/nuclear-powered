package io.github.ash1688.nuclearpowered.block.creative;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

// Infinite FE source for testing the dual-energy system. Each tick, pushes
// Integer.MAX_VALUE FE to every adjacent IEnergyStorage neighbour that's
// willing to receive — neighbour decides its own per-tick cap.
public class CreativeFEGeneratorBlockEntity extends BlockEntity {
    public CreativeFEGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FE_GENERATOR.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            IEnergyStorage sink = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
            if (sink == null || !sink.canReceive()) continue;
            sink.receiveEnergy(Integer.MAX_VALUE, false);
        }
    }
}
