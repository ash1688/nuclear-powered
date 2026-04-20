package io.github.ash1688.nuclearpowered.block.watersource;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

// Testing/creative helper. Pushes up to FILL_PER_TICK_MB water into every
// adjacent block that exposes an IFluidHandler capable of receiving water.
// No GUI, no internal tank, no cost — drop it next to any machine that needs
// water and that tank stays topped up.
public class InfiniteWaterSourceBlockEntity extends BlockEntity {
    private static final int FILL_PER_TICK_MB = 1000;

    public InfiniteWaterSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_WATER_SOURCE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        FluidStack offer = new FluidStack(Fluids.WATER, FILL_PER_TICK_MB);
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                    .ifPresent(sink -> sink.fill(offer, IFluidHandler.FluidAction.EXECUTE));
        }
    }
}
