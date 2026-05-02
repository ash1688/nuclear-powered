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
//
// Dual-energy mode: each cable also tracks an effective mode (FE / EU)
// determined by its connected neighbours (FE wins ties). Cables in EU mode
// don't expose Forge ENERGY — neighbours of the wrong mode refuse to connect
// at the cap-resolution layer, which is exactly the visual / functional
// refuse-connect spec. Real EU transport (distributeEU) is a follow-up.
public class EnergyCableBlockEntity extends BlockEntity {
    // Bound BFS so a pathologically large network can't stall the server thread.
    public static final int MAX_NETWORK_SIZE = 512;

    /**
     * Per-face wrapper. Each face exposes a separate {@link IEnergyStorage}
     * so when a neighbour pushes FE into the cable, we know <em>which</em>
     * face it came from. The source face's neighbour is then excluded from
     * the network distribution so we never route the FE straight back into
     * the producer (the loop bug that left batteries oscillating in place
     * with their storedFE never actually growing).
     */
    private final IEnergyStorage[] perFaceWrappers = new IEnergyStorage[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IEnergyStorage>[] perFaceLazies = new LazyOptional[6];

    /** Side-agnostic wrapper used when callers query the cap with side==null.
     *  Treats the call as having no source face — distributes to every buffer
     *  in the network. Vanilla rarely does this, but Forge sometimes does for
     *  pre-flight cap checks. */
    private final IEnergyStorage anySideWrapper = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide || amount <= 0) return 0;
            return distribute(amount, simulate, null);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> lazyAnySide = LazyOptional.empty();

    /** Lazy GT IEnergyContainer wrapper. Initialised in onLoad when GTCEU is
     *  loaded; stays empty otherwise. Exposed by getCapability when this
     *  cable is in EU mode so GT producers can push energy into the
     *  network. */
    @SuppressWarnings("rawtypes")
    private LazyOptional lazyEU = LazyOptional.empty();

    /** Current mode this cable speaks. Recomputed from neighbours every time
     *  a face's connection state changes (see {@link #recomputeMode}). FE
     *  wins ties when both an FE and EU producer are connected. */
    private io.github.ash1688.nuclearpowered.energy.EnergyMode mode =
            io.github.ash1688.nuclearpowered.energy.EnergyMode.FE;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    public io.github.ash1688.nuclearpowered.energy.EnergyMode getMode() {
        return mode;
    }

    /**
     * Walk the 6 neighbours and pick the cable's mode. FE neighbours win;
     * EU is chosen only when no FE neighbour is present. Cable-to-cable
     * adopts the neighbour cable's existing mode so a chain stays coherent
     * even if the producer is several blocks away. Called from the block's
     * updateShape; idempotent — only invalidates caps when the mode changes.
     */
    public void recomputeMode() {
        if (level == null) return;
        boolean anyFE = false;
        boolean anyEU = false;
        for (Direction dir : Direction.values()) {
            BlockPos npos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(npos);
            if (be == null) continue;
            if (be instanceof EnergyCableBlockEntity nb) {
                if (nb.mode == io.github.ash1688.nuclearpowered.energy.EnergyMode.FE) anyFE = true;
                else anyEU = true;
                continue;
            }
            if (be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent()) {
                anyFE = true;
                continue;
            }
            if (io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat.isLoaded()
                    && io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat
                            .hasEUCapability(be, dir.getOpposite())) {
                anyEU = true;
            }
        }
        io.github.ash1688.nuclearpowered.energy.EnergyMode next =
                anyFE ? io.github.ash1688.nuclearpowered.energy.EnergyMode.FE
              : anyEU ? io.github.ash1688.nuclearpowered.energy.EnergyMode.EU
              : mode; // no neighbours: keep current — avoids needless cap churn
        if (next != mode) {
            mode = next;
            invalidateCaps();
            lazyAnySide = LazyOptional.of(() -> anySideWrapper);
            for (int i = 0; i < perFaceLazies.length; i++) perFaceLazies[i] = null;
            setChanged();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        // EU mode: serve the GT IEnergyContainer cap; Forge ENERGY is hidden
        // so wrong-mode neighbours refuse to resolve a connection.
        if (mode == io.github.ash1688.nuclearpowered.energy.EnergyMode.EU) {
            if (cap == ForgeCapabilities.ENERGY) return LazyOptional.empty();
            if (io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat.isLoaded()
                    && io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat
                            .isEnergyContainerCap(cap)) {
                return (LazyOptional<T>) lazyEU;
            }
            return super.getCapability(cap, side);
        }
        // FE mode: existing per-face FE wrappers.
        if (cap != ForgeCapabilities.ENERGY) return super.getCapability(cap, side);
        if (side == null) return lazyAnySide.cast();
        int idx = side.ordinal();
        if (perFaceLazies[idx] == null) perFaceLazies[idx] = LazyOptional.of(() -> getOrCreateFaceWrapper(side));
        return perFaceLazies[idx].cast();
    }

    /**
     * Build (and memoise) the per-face wrapper. Each face's receiveEnergy
     * passes its own direction down to {@link #distribute} so that
     * {@link #discoverNetwork} can skip the BlockEntity at the source
     * position from the buffer list.
     */
    private IEnergyStorage getOrCreateFaceWrapper(Direction face) {
        int idx = face.ordinal();
        IEnergyStorage existing = perFaceWrappers[idx];
        if (existing != null) return existing;
        IEnergyStorage wrapper = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                if (level == null || level.isClientSide || amount <= 0) return 0;
                return distribute(amount, simulate, face);
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return 0; }
            @Override public int getMaxEnergyStored() { return 0; }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() { return true; }
        };
        perFaceWrappers[idx] = wrapper;
        return wrapper;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyAnySide = LazyOptional.of(() -> anySideWrapper);
        if (io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat.isLoaded()) {
            lazyEU = io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat.wrapCableAsEU(this);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("energyMode", mode.name());
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energyMode")) {
            try {
                mode = io.github.ash1688.nuclearpowered.energy.EnergyMode.valueOf(tag.getString("energyMode"));
            } catch (IllegalArgumentException ignored) {
                mode = io.github.ash1688.nuclearpowered.energy.EnergyMode.FE;
            }
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyAnySide.invalidate();
        lazyEU.invalidate();
        for (int i = 0; i < perFaceLazies.length; i++) {
            if (perFaceLazies[i] != null) perFaceLazies[i].invalidate();
        }
    }

    /**
     * BFS the connected cable network and push {@code amount} into the
     * non-cable consumers we discovered. Pure sinks (canReceive && !canExtract:
     * furnace, crusher, etc.) go first so a battery next to a furnace can't
     * swallow all the FE while the furnace starves. Leftover FE spills into
     * buffers (batteries) via their own receiveEnergy cap.
     *
     * <p>{@code sourceFace} is the direction from which this cable received
     * the FE — that neighbour is excluded from the buffer/sink list so we
     * never feed FE back into the block that just pushed it to us.</p>
     */
    private int distribute(int amount, boolean simulate, @Nullable Direction sourceFace) {
        List<IEnergyStorage> pureSinks = new ArrayList<>();
        List<IEnergyStorage> buffers = new ArrayList<>();
        BlockPos sourcePos = sourceFace == null ? null : worldPosition.relative(sourceFace);
        discoverNetwork(pureSinks, buffers, sourcePos);

        int remaining = pushInto(pureSinks, amount, simulate);
        if (remaining > 0) remaining = pushIntoEvenly(buffers, remaining, simulate);
        return amount - remaining;
    }

    /** Greedy push: each sink takes as much as it can in turn. Used for pure
     *  sinks where we WANT the first ones reached to fill up immediately. */
    private int pushInto(List<IEnergyStorage> sinks, int remaining, boolean simulate) {
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) break;
            remaining -= sink.receiveEnergy(remaining, simulate);
        }
        return remaining;
    }

    /**
     * Even-share push: divide {@code remaining} among the sinks, give each
     * its share, then sweep up any leftover (from sinks that were full or
     * rate-limited) into the next sink that still has room. Two buffers on
     * the same cable network fill at roughly the same rate instead of one
     * hoarding everything until the other can finally start.
     */
    private int pushIntoEvenly(List<IEnergyStorage> sinks, int remaining, boolean simulate) {
        if (sinks.isEmpty() || remaining <= 0) return remaining;
        int n = sinks.size();
        int perSink = Math.max(1, remaining / n);
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) break;
            int share = Math.min(remaining, perSink);
            remaining -= sink.receiveEnergy(share, simulate);
        }
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) break;
            remaining -= sink.receiveEnergy(remaining, simulate);
        }
        return remaining;
    }

    /**
     * EU pathway parallel to {@link #distribute}. A GT producer pushes a
     * voltage × amperage packet into our IEnergyContainer wrapper; we BFS
     * the cable network and offer those amperage packets to each EU sink in
     * turn until either every sink is full or the producer's amperage is
     * exhausted. Returns the amperage actually accepted.
     *
     * <p>Source-face exclusion mirrors the FE side — the neighbour that
     * pushed the packet doesn't appear in the sink list, so we never feed
     * it back into the producer.</p>
     */
    public long distributeEU(@Nullable Direction sourceFace, long voltage, long amperage) {
        if (level == null || level.isClientSide || amperage <= 0 || voltage <= 0) return 0;
        if (!io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat.isLoaded()) return 0;
        BlockPos sourcePos = sourceFace == null ? null : worldPosition.relative(sourceFace);
        // Sinks are stored as (BlockEntity, facing) pairs because GT's
        // pushEUToSink takes the receiving face. Same neighbour reachable
        // from multiple cables only counts once (consumersSeen guard).
        List<EUSinkEntry> sinks = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> consumersSeen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos npos = cur.relative(dir);
                if (sourcePos != null && npos.equals(sourcePos)) continue;
                BlockEntity be = level.getBlockEntity(npos);
                if (be == null) continue;
                if (be instanceof EnergyCableBlockEntity nb) {
                    // Only walk into cables in the same EU mode — wrong-mode
                    // cables aren't part of this network.
                    if (nb.mode != io.github.ash1688.nuclearpowered.energy.EnergyMode.EU) continue;
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }
                if (!consumersSeen.add(npos)) continue;
                // Only neighbours that actually accept EU on the touching face.
                if (io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat
                        .hasEUCapability(be, dir.getOpposite())) {
                    sinks.add(new EUSinkEntry(be, dir.getOpposite()));
                }
            }
        }
        long remaining = amperage;
        for (EUSinkEntry sink : sinks) {
            if (remaining <= 0) break;
            long taken = io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat
                    .pushEUToSink(sink.be, sink.facing, voltage, remaining);
            if (taken > 0) remaining -= taken;
        }
        return amperage - remaining;
    }

    private record EUSinkEntry(BlockEntity be, Direction facing) {}

    private void discoverNetwork(List<IEnergyStorage> pureSinks, List<IEnergyStorage> buffers,
                                  @Nullable BlockPos exclude) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> consumersSeen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos npos = cur.relative(dir);
                // Skip the source position so a producer pushing into the
                // network doesn't end up receiving its own push back.
                if (exclude != null && npos.equals(exclude)) continue;
                BlockEntity be = level.getBlockEntity(npos);
                if (be == null) continue;
                if (be instanceof EnergyCableBlockEntity) {
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }
                // A single non-cable machine might border the network from several
                // cables; only probe each position once.
                if (!consumersSeen.add(npos)) continue;
                // Skip GT-aware neighbours — their Forge ENERGY shim silently
                // voids FE when the EU side can't actually accept (battery
                // buffer with no batteries inserted, full machine, etc.).
                // Players who want to bridge to GT route via the dedicated
                // FE↔EU converter, which speaks both protocols.
                if (io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat.isLoaded()
                        && io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat
                                .isExternalGTSink(be, dir.getOpposite())) continue;
                be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                    if (!cap.canReceive()) return;
                    if (cap.canExtract()) buffers.add(cap);
                    else pureSinks.add(cap);
                });
            }
        }
    }
}
