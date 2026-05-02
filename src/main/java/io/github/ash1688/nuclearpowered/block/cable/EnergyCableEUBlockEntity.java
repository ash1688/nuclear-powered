package io.github.ash1688.nuclearpowered.block.cable;

import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// EU-only cable. Parallel to EnergyCableBlockEntity (the FE cable) — same
// network-discovery shape, but speaks GT's IEnergyContainer instead of Forge
// ENERGY. EU producers push amperage packets into our adapter; we BFS the
// connected EU-cable network and dispatch to consumers exposing GT EU caps.
//
// No FE wrappers, no per-face FE buffers — EU cables refuse all FE traffic,
// FE cables do the same in reverse. The two networks never touch.
public class EnergyCableEUBlockEntity extends BlockEntity {
    public static final int MAX_NETWORK_SIZE = 512;

    /** Lazy GT IEnergyContainer wrapper. Initialised in onLoad when GTCEU is
     *  loaded; stays empty otherwise (block places but is inert). */
    @SuppressWarnings("rawtypes")
    private LazyOptional lazyEU = LazyOptional.empty();

    public EnergyCableEUBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE_EU.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (GTCompat.isLoaded()) {
            lazyEU = GTEnergyCompat.wrapEUCableAsContainer(this);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEU.invalidate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        // Refuse Forge ENERGY explicitly so external shims (GTCEU's auto
        // attach, etc.) can't trick FE cables into connecting through us.
        if (cap == ForgeCapabilities.ENERGY) return LazyOptional.empty();
        if (GTCompat.isLoaded() && GTEnergyCompat.isEnergyContainerCap(cap)) {
            return (LazyOptional<T>) lazyEU;
        }
        return super.getCapability(cap, side);
    }

    /**
     * Push EU through the cable network. A producer's
     * {@code acceptEnergyFromNetwork} call lands in our adapter, which
     * forwards here; we walk connected EU cables, find EU-cap consumers,
     * and offer them the amperage in turn.
     */
    public long distributeEU(@Nullable Direction sourceFace, long voltage, long amperage) {
        if (level == null || level.isClientSide || amperage <= 0 || voltage <= 0) return 0;
        if (!GTCompat.isLoaded()) return 0;
        BlockPos sourcePos = sourceFace == null ? null : worldPosition.relative(sourceFace);
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
                if (be instanceof EnergyCableEUBlockEntity) {
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }
                if (!consumersSeen.add(npos)) continue;
                if (GTEnergyCompat.hasEUCapability(be, dir.getOpposite())) {
                    sinks.add(new EUSinkEntry(be, dir.getOpposite()));
                }
            }
        }
        long remaining = amperage;
        for (EUSinkEntry sink : sinks) {
            if (remaining <= 0) break;
            long taken = GTEnergyCompat.pushEUToSink(sink.be, sink.facing, voltage, remaining);
            if (taken > 0) remaining -= taken;
        }
        return amperage - remaining;
    }

    private record EUSinkEntry(BlockEntity be, Direction facing) {}
}
