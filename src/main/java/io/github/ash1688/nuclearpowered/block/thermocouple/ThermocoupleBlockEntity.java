package io.github.ash1688.nuclearpowered.block.thermocouple;

import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.menu.ThermocoupleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class ThermocoupleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY_FE = 10_000;
    public static final int MAX_OUTPUT_FE_PER_TICK = 256;
    // Conversion ratio: 10 heat = 1 FE/tick. An 0-casing pile at ~933 heat yields
    // ~93 FE/tick; a 26-casing pile around ~3360 heat yields ~336 FE/tick.
    private static final int HEAT_PER_FE = 10;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_DISTANCE = 64;

    // Internal energy counter. Exposed externally as extract-only via `externalEnergy`.
    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int take = Math.min(storedFE, Math.min(maxExtract, MAX_OUTPUT_FE_PER_TICK));
            if (take <= 0) return 0;
            if (!simulate) {
                storedFE -= take;
                setChanged();
            }
            return take;
        }

        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return CAPACITY_FE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    @Nullable private BlockPos cachedPilePos;
    private int scanCooldown = 0;
    private int lastGenerationFE = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> storedFE;
                case 1 -> CAPACITY_FE;
                case 2 -> cachedPilePos != null ? 1 : 0;
                case 3 -> lastGenerationFE;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only indices.
        }

        @Override
        public int getCount() { return 4; }
    };

    public ThermocoupleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMOCOUPLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.thermocouple");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ThermocoupleMenu(id, inv, this, data);
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

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Rediscover the connected pile periodically. If the cached position has been
        // broken or replaced, drop it and re-scan.
        if (cachedPilePos != null) {
            BlockState cached = level.getBlockState(cachedPilePos);
            if (!cached.is(ModBlocks.GRAPHITE_PILE.get())) cachedPilePos = null;
        }
        if (scanCooldown-- <= 0 || cachedPilePos == null) {
            findConnectedPile(level);
            scanCooldown = SCAN_INTERVAL_TICKS;
        }

        // Generate FE from pile heat. Also cool the pile by the amount of heat we
        // actually converted — otherwise heat only falls via the pile's own passive
        // decay, and players can't use thermocouples to manage reactor temperature.
        lastGenerationFE = 0;
        if (cachedPilePos != null && storedFE < CAPACITY_FE) {
            BlockEntity be = level.getBlockEntity(cachedPilePos);
            if (be instanceof PileBlockEntity pile) {
                int heat = pile.getHeat();
                int produced = Math.max(0, heat / HEAT_PER_FE);
                int canAccept = Math.min(produced, CAPACITY_FE - storedFE);
                if (canAccept > 0) {
                    storedFE += canAccept;
                    lastGenerationFE = canAccept;
                    pile.drainHeat(canAccept * HEAT_PER_FE);
                }
            }
        }

        // Push FE to adjacent consumers. Target is each neighbour's ENERGY capability
        // queried from the side facing us.
        if (storedFE > 0) {
            for (Direction dir : Direction.values()) {
                if (storedFE <= 0) break;
                BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
                if (neighbour == null) continue;
                neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(sink -> {
                    if (!sink.canReceive()) return;
                    int offered = Math.min(storedFE, MAX_OUTPUT_FE_PER_TICK);
                    int accepted = sink.receiveEnergy(offered, false);
                    if (accepted > 0) {
                        storedFE -= accepted;
                    }
                });
            }
        }

        if (lastGenerationFE > 0 || storedFE != 0) {
            setChanged(level, pos, state);
        }
    }

    // Flood-fill from the thermocouple's neighbours through graphite_casing blocks until
    // a graphite_pile is hit. The first pile found wins (which is fine for MVP even if
    // multiple piles are interconnected — future polish can track the closest).
    private void findConnectedPile(Level level) {
        cachedPilePos = null;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            queue.offer(worldPosition.relative(dir));
        }
        while (!queue.isEmpty() && visited.size() < SCAN_DISTANCE) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) continue;
            BlockState bs = level.getBlockState(p);
            if (bs.is(ModBlocks.GRAPHITE_PILE.get())) {
                cachedPilePos = p;
                return;
            }
            if (bs.is(ModBlocks.GRAPHITE_CASING.get())) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = p.relative(dir);
                    if (!visited.contains(next)) queue.offer(next);
                }
            }
        }
    }
}
