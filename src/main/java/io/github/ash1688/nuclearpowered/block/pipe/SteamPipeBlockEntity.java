package io.github.ash1688.nuclearpowered.block.pipe;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Network-based steam conduit. Mirrors EnergyCableBlockEntity: no per-pipe buffer,
// one synchronous pass-through per push. When a boiler (or anything else) fills
// steam into a pipe, that fill is BFS-distributed across all non-pipe fluid sinks
// reachable through the connected pipe network.
public class SteamPipeBlockEntity extends BlockEntity {
    public static final int MAX_NETWORK_SIZE = 512;

    private final IFluidHandler passThrough = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 0; }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid() == ModFluids.STEAM.get();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (level == null || level.isClientSide || resource.isEmpty()) return 0;
            if (resource.getFluid() != ModFluids.STEAM.get()) return 0;
            return distribute(resource, action);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    public SteamPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_PIPE.get(), pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluidHandler = LazyOptional.of(() -> passThrough);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
    }

    // BFS the connected pipe network and push `resource` into each non-pipe fluid
    // handler in turn until the offer is exhausted or every sink is full.
    private int distribute(FluidStack resource, IFluidHandler.FluidAction action) {
        List<IFluidHandler> sinks = new ArrayList<>();
        discoverNetwork(sinks);
        int remaining = resource.getAmount();
        for (IFluidHandler sink : sinks) {
            if (remaining <= 0) break;
            FluidStack offer = new FluidStack(resource, remaining);
            remaining -= sink.fill(offer, action);
        }
        return resource.getAmount() - remaining;
    }

    private void discoverNetwork(List<IFluidHandler> sinks) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos npos = cur.relative(dir);
                BlockEntity be = level.getBlockEntity(npos);
                if (be == null) continue;
                if (be instanceof SteamPipeBlockEntity) {
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }
                if (!seen.add(npos)) continue;
                be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(sinks::add);
            }
        }
    }
}
