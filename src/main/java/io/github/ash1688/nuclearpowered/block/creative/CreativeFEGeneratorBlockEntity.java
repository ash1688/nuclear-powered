package io.github.ash1688.nuclearpowered.block.creative;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

// Infinite FE source for testing the dual-energy system. Each tick, pushes
// FE_PER_TICK FE per face into any adjacent IEnergyStorage that canReceive.
// Throttled rather than uncapped so the buffer bar fills visibly during
// testing — bumping the rate is one constant change away.
public class CreativeFEGeneratorBlockEntity extends BlockEntity {
    /** 200 FE/sec — fills a typical 10k machine buffer in ~50 seconds. Slow
     *  enough that you can clearly see the bar climb during a test. */
    private static final int FE_PER_TICK = 10;

    // Phantom IEnergyStorage so cables visually recognise this block as an
    // FE endpoint and connect their arms to it. Real flow goes the other way
    // (the tick below pushes into neighbours), so receiveEnergy / extractEnergy
    // here are stubs — they exist only to satisfy the cable's connection check.
    private final IEnergyStorage phantom = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return maxExtract; }
        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IEnergyStorage> lazyPhantom = LazyOptional.empty();

    public CreativeFEGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FE_GENERATOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyPhantom = LazyOptional.of(() -> phantom);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyPhantom.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyPhantom.cast();
        return super.getCapability(cap, side);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            IEnergyStorage sink = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
            if (sink == null || !sink.canReceive()) continue;
            sink.receiveEnergy(FE_PER_TICK, false);
        }
    }
}
