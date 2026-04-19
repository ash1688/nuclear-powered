package io.github.ash1688.nuclearpowered.block.cable;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Network-based conduit. Cables do not buffer FE — when a producer (thermocouple,
// steam engine, battery) pushes into a cable, that push is distributed immediately
// across all non-cable consumers reachable through the connected cable network.
// This mirrors how Forge/RF pipe mods (Thermal, Mekanism, Flux) route energy:
// one transfer per push, producer → network → consumers, no per-cable storage and
// therefore no ordering or oscillation bugs.
public class EnergyCableBlockEntity extends BlockEntity {
    // Bound BFS so a pathologically large network can't stall the server thread.
    public static final int MAX_NETWORK_SIZE = 512;

    private final IEnergyStorage passThrough = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide || amount <= 0) return 0;
            return distribute(amount, simulate);
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
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
        lazyEnergy = LazyOptional.of(() -> passThrough);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }

    // BFS the connected cable network, then push `amount` into the non-cable
    // consumers we discovered. Pure sinks (canReceive && !canExtract: furnace,
    // crusher, etc.) go first so a battery next to a furnace can't swallow all
    // the FE while the furnace starves. Leftover FE spills into buffers
    // (batteries) via their own receiveEnergy cap.
    private int distribute(int amount, boolean simulate) {
        List<IEnergyStorage> pureSinks = new ArrayList<>();
        List<IEnergyStorage> buffers = new ArrayList<>();
        discoverNetwork(pureSinks, buffers);

        int remaining = pushInto(pureSinks, amount, simulate);
        if (remaining > 0) remaining = pushInto(buffers, remaining, simulate);
        return amount - remaining;
    }

    private int pushInto(List<IEnergyStorage> sinks, int remaining, boolean simulate) {
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) break;
            remaining -= sink.receiveEnergy(remaining, simulate);
        }
        return remaining;
    }

    private void discoverNetwork(List<IEnergyStorage> pureSinks, List<IEnergyStorage> buffers) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> consumersSeen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos npos = cur.relative(dir);
                BlockEntity be = level.getBlockEntity(npos);
                if (be == null) continue;
                if (be instanceof EnergyCableBlockEntity) {
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }
                // A single non-cable machine might border the network from several
                // cables; only probe each position once.
                if (!consumersSeen.add(npos)) continue;
                be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                    if (!cap.canReceive()) return;
                    if (cap.canExtract()) buffers.add(cap);
                    else pureSinks.add(cap);
                });
            }
        }
    }
}
