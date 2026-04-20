package io.github.ash1688.nuclearpowered.block.battery;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.menu.BatteryMenu;
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

public class BatteryBlockEntity extends BlockEntity implements MenuProvider {
    // Sized to hold ~4 full reprocessing cycles' worth (1 cycle ~240K FE across
    // the five machines). One battery won't power the full chain in parallel
    // (MAX_IO_PER_TICK < 1200 FE/tick chain draw), but it'll comfortably buffer
    // surpluses overnight and smooth out thermo output gaps.
    public static final int CAPACITY_FE = 1_000_000;
    public static final int MAX_IO_PER_TICK = 1024;

    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accept = Math.min(CAPACITY_FE - storedFE, Math.min(amount, MAX_IO_PER_TICK));
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

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> storedFE;
                case 1 -> CAPACITY_FE;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the menu's perspective.
        }

        @Override
        public int getCount() { return 2; }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BatteryMenu(id, inv, this, data);
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
        if (level.isClientSide || storedFE <= 0) return;
        for (Direction dir : Direction.values()) {
            if (storedFE <= 0) break;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                if (!sink.canReceive()) return 0;
                int offered = Math.min(storedFE, MAX_IO_PER_TICK);
                return sink.receiveEnergy(offered, false);
            }).orElse(0);
            if (delta > 0) {
                storedFE -= delta;
                setChanged();
            }
        }
    }
}
