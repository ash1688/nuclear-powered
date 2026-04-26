package io.github.ash1688.nuclearpowered.block.thermocouple;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
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

public class ThermocoupleBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int CAPACITY_FE = 10_000;
    // Cap raised above the 5K sweet-spot raw output (500 FE/tick) so the
    // efficiency curve below can actually swing values up and down without
    // the cap silently flattening them.
    public static final int MAX_OUTPUT_FE_PER_TICK = 600;
    // Conversion ratio: 10 heat = 1 FE/tick base, before efficiency multiplier.
    private static final int HEAT_PER_FE = 10;
    // Each thermo cools the connected pile once per second:
    //   idle     -1 H/s (attached, nothing drew FE this second)
    //   active   -2 H/s (someone pulled FE this second)
    //   coolant  -5 H/s (player toggled coolant mode; no FE output)
    private static final int COOL_INTERVAL_TICKS = 20;
    private static final int COOL_IDLE_PER_INTERVAL = 1;
    private static final int COOL_ACTIVE_PER_INTERVAL = 2;
    private static final int COOL_COOLANT_PER_INTERVAL = 5;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_DISTANCE = 64;

    // Internal energy counter. Exposed externally as extract-only via `externalEnergy`.
    private int storedFE = 0;
    private int coolTickCounter = 0;
    private boolean extractedThisInterval = false;
    private boolean coolantMode = false;
    // Fermi-III Exchange: Heat Capture Efficiency Core. Permanent +10 % on the
    // FE this thermo produces at every heat zone. Persisted in NBT.
    private boolean heatCaptureEfficiency = false;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            // Coolant mode dumps heat but refuses to push FE downstream.
            if (coolantMode) return 0;
            int take = Math.min(storedFE, Math.min(maxExtract, MAX_OUTPUT_FE_PER_TICK));
            if (take <= 0) return 0;
            if (!simulate) {
                storedFE -= take;
                extractedThisInterval = true;
                setChanged();
            }
            return take;
        }

        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return CAPACITY_FE; }
        @Override public boolean canExtract() { return !coolantMode; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    @Nullable private BlockPos cachedPilePos;
    private int scanCooldown = 0;
    private int lastGenerationFE = 0;

    public ThermocoupleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMOCOUPLE.get(), pos, state);
    }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.thermocouple");

        ui.mainGroup.addWidget(NPMachineUI.feBar(82, 17,
                () -> storedFE, CAPACITY_FE));

        // Coolant-mode toggle stays inline — it's a unique mode switch, not
        // a generic auto-I/O. PANEL_X manually applied since toggleButton
        // takes absolute coords.
        ui.mainGroup.addWidget(NPMachineUI.toggleButton(NPMachineUI.PANEL_X + 8, 58, 80,
                "Coolant Mode", () -> coolantMode, this::toggleCoolantMode));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        return ui;
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
        tag.putBoolean("coolantMode", coolantMode);
        tag.putBoolean("heatCaptureEfficiency", heatCaptureEfficiency);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedFE = tag.getInt("fe");
        coolantMode = tag.getBoolean("coolantMode");
        heatCaptureEfficiency = tag.getBoolean("heatCaptureEfficiency");
    }

    public boolean hasHeatCaptureEfficiency() { return heatCaptureEfficiency; }

    public boolean applyHeatCaptureEfficiency() {
        if (heatCaptureEfficiency) return false;
        heatCaptureEfficiency = true;
        setChanged();
        return true;
    }

    public boolean isCoolantMode() { return coolantMode; }

    public void toggleCoolantMode() {
        coolantMode = !coolantMode;
        setChanged();
    }

    // FE produced per tick given current pile heat and zone efficiency.
    // Multipliers: <3K = 0.5, 3K-5K = 1.0 (sweet spot), 5K-7K = 0.7, 7K+ = 0.1
    // (softened to 0.25 if the pile has a Thermal Dampener installed).
    // The 7K+ floor is steep so overheated piles tank output — gives the
    // player real incentive to keep the reactor in the sweet spot.
    // Heat Capture Efficiency Core on this thermo multiplies the final output
    // by 1.10 at every zone.
    private int efficiencyScaledProduction(int heat, boolean pileHasThermalDampener) {
        if (heat <= 0) return 0;
        int baseFE = heat / HEAT_PER_FE;
        int percent;
        if (heat < 3000)       percent = 50;
        else if (heat < 5000)  percent = 100;
        else if (heat < 7000)  percent = 70;
        else                   percent = pileHasThermalDampener ? 25 : 10;
        int produced = baseFE * percent / 100;
        if (heatCaptureEfficiency) produced = produced * 110 / 100;
        return produced;
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

        // Generate FE from pile heat, scaled by a zone efficiency multiplier
        // so players get the best yield in the 3K-5K sweet spot.
        lastGenerationFE = 0;
        if (cachedPilePos != null && storedFE < CAPACITY_FE) {
            BlockEntity be = level.getBlockEntity(cachedPilePos);
            if (be instanceof PileBlockEntity pile) {
                int heat = pile.getHeat();
                int produced = efficiencyScaledProduction(heat, pile.hasThermalDampener());
                int canAccept = Math.min(produced, CAPACITY_FE - storedFE);
                if (canAccept > 0) {
                    storedFE += canAccept;
                    lastGenerationFE = canAccept;
                }
            }
        }

        // Cool the pile once per second. Rate depends on thermo state:
        //   coolant mode -> -5/s (no FE pushed out)
        //   active       -> -2/s (FE was extracted this window)
        //   idle         -> -1/s
        coolTickCounter++;
        if (coolTickCounter >= COOL_INTERVAL_TICKS) {
            coolTickCounter = 0;
            if (cachedPilePos != null) {
                BlockEntity be = level.getBlockEntity(cachedPilePos);
                if (be instanceof PileBlockEntity pile) {
                    int drain = coolantMode ? COOL_COOLANT_PER_INTERVAL
                            : (extractedThisInterval ? COOL_ACTIVE_PER_INTERVAL : COOL_IDLE_PER_INTERVAL);
                    pile.drainHeat(drain);
                }
            }
            extractedThisInterval = false;
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
