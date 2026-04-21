package io.github.ash1688.nuclearpowered.block.watersource;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

// Creative/test helper. Pushes up to FILL_PER_TICK_MB of nitric acid into
// every adjacent fluid handler that accepts it. No GUI, no cost.
public class InfiniteNitricAcidSourceBlockEntity extends BlockEntity {
    private static final int FILL_PER_TICK_MB = 1000;

    public InfiniteNitricAcidSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_NITRIC_ACID_SOURCE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        FluidStack offer = new FluidStack(ModFluids.NITRIC_ACID.get(), FILL_PER_TICK_MB);
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                    .ifPresent(sink -> sink.fill(offer, IFluidHandler.FluidAction.EXECUTE));
        }
    }
}
