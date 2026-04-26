package io.github.ash1688.nuclearpowered.block.battery;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.mojang.logging.LogUtils;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
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

public class BatteryBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /** TEMPORARY debug interval (ticks). 0 = off. */
    private static final int DEBUG_PUSH_LOG_INTERVAL = 40;
    private int debugTick = 0;

    // Holds ~20 reprocessing cycles' worth (1 cycle ~240K FE). IO ceiling
    // 2048/tick means a single battery comfortably covers the 1200 FE/tick
    // reprocessing chain on its own, with headroom.
    public static final int CAPACITY_FE = 5_000_000;
    public static final int MAX_IO_PER_TICK = 2048;

    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accept = Math.min(CAPACITY_FE - storedFE, Math.min(amount, MAX_IO_PER_TICK));
            if (DEBUG_PUSH_LOG_INTERVAL > 0 && !simulate && accept > 0) {
                LOGGER.info("[NP-Battery] receiveEnergy accepted {} (stored {} -> {})",
                        accept, storedFE, storedFE + accept);
            }
            if (accept <= 0) return 0;
            if (!simulate) {
                storedFE += accept;
                setChanged();
            }
            return accept;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int take = Math.min(storedFE, Math.min(maxExtract, MAX_IO_PER_TICK));
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
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
    }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.battery");

        // Centred FE bar — the only thing this machine has to display.
        ui.mainGroup.addWidget(NPMachineUI.feBar(82, 17,
                () -> storedFE, CAPACITY_FE));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        // Tab strip is reserved for future side-config / energy-direction tabs.
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedFE = tag.getInt("fe");
    }

    // Push stored FE to any adjacent consumer each tick. Sources (thermocouples) push
    // INTO the battery via the capability wrapper; the battery pushes OUT to cables /
    // machines. Minor oscillation with neighbouring cables is harmless (no FE created
    // or destroyed) and FE still reaches real sinks within a few ticks.
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean log = DEBUG_PUSH_LOG_INTERVAL > 0
                && (debugTick++ % DEBUG_PUSH_LOG_INTERVAL == 0);
        if (log) LOGGER.info("[NP-Battery] tick — storedFE={}, pos={}", storedFE, pos);
        if (storedFE <= 0) return;
        for (Direction dir : Direction.values()) {
            if (storedFE <= 0) break;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            // Skip GT-aware neighbours — battery buffers and similar GT
            // blocks expose a Forge ENERGY shim that silently voids FE when
            // their EU side can't actually accept (e.g. battery buffer with
            // no batteries). Players who want to bridge to GT should put a
            // dedicated FE↔EU converter between the NP network and GT.
            boolean gtLoaded = GTCompat.isLoaded();
            boolean isExternalGT = gtLoaded
                    && GTEnergyCompat.isExternalGTSink(neighbour, dir.getOpposite());
            if (log) LOGGER.info("[NP-Battery]   {} -> {} (externalGTSink={})",
                    dir, neighbour.getClass().getName(), isExternalGT);
            if (isExternalGT) continue;
            int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                if (!sink.canReceive()) return 0;
                int offered = Math.min(storedFE, MAX_IO_PER_TICK);
                return sink.receiveEnergy(offered, false);
            }).orElse(0);
            if (log) LOGGER.info("[NP-Battery]     pushed {} FE", delta);
            if (delta > 0) {
                storedFE -= delta;
                setChanged();
            }
        }
    }
}
