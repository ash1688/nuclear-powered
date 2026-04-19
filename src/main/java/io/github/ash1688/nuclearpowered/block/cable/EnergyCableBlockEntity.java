package io.github.ash1688.nuclearpowered.block.cable;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class EnergyCableBlockEntity extends BlockEntity {
    public static final int CAPACITY = 500;
    // Per-face transfer ceiling. Matches the thermocouple's push rate so a cable never
    // becomes the bottleneck for a single generator.
    public static final int TRANSFER_RATE = 256;

    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accept = Math.min(CAPACITY - storedFE, Math.min(amount, TRANSFER_RATE));
            if (accept <= 0) return 0;
            if (!simulate) {
                storedFE += accept;
                setChanged();
            }
            return accept;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int take = Math.min(storedFE, Math.min(maxExtract, TRANSFER_RATE));
            if (take <= 0) return 0;
            if (!simulate) {
                storedFE -= take;
                setChanged();
            }
            return take;
        }

        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("fe", storedFE);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedFE = tag.getInt("fe");
    }

    public int getStoredFE() { return storedFE; }

    // Cable tick: two passes so FE flows forward instead of oscillating back into
    // sources. Pass 1 drains into terminal consumers (canReceive && !canExtract) —
    // furnaces, crushers, washers. Pass 2 delivers to buffer blocks (canExtract too:
    // cables, batteries) but only when their stored FE is strictly lower than ours,
    // so a full battery or an equal-fill cable can't flow back in the same tick.
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || storedFE <= 0) return;
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (storedFE <= 0) break;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            final int snapshot = storedFE;
            int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                if (!sink.canReceive() || sink.canExtract()) return 0;
                return sink.receiveEnergy(Math.min(snapshot, TRANSFER_RATE), false);
            }).orElse(0);
            if (delta > 0) {
                storedFE -= delta;
                changed = true;
            }
        }
        // Non-cable buffers (batteries) get priority so direction iteration can't
        // dump the whole buffer into a sideways cable and starve the real destination.
        changed |= pushToBuffers(level, pos, false);
        changed |= pushToBuffers(level, pos, true);
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private boolean pushToBuffers(Level level, BlockPos pos, boolean cablesOnly) {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (storedFE <= 0) break;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            boolean isCable = neighbour instanceof EnergyCableBlockEntity;
            if (isCable != cablesOnly) continue;
            int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                if (!sink.canReceive() || !sink.canExtract()) return 0;
                int other = sink.getEnergyStored();
                if (other >= storedFE) return 0;
                int offered = Math.min(Math.min(storedFE, TRANSFER_RATE), storedFE - other);
                return sink.receiveEnergy(offered, false);
            }).orElse(0);
            if (delta > 0) {
                storedFE -= delta;
                changed = true;
            }
        }
        return changed;
    }
}
