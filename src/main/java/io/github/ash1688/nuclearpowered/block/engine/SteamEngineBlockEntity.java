package io.github.ash1688.nuclearpowered.block.engine;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import io.github.ash1688.nuclearpowered.menu.SteamEngineMenu;
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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class SteamEngineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int STEAM_CAPACITY_MB = 4000;
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_OUTPUT_FE_PER_TICK = 512;

    // Conversion rate: each tick, consume STEAM_PER_TICK mB of steam and produce
    // FE_PER_STEAM_TICK FE. 25 FE/tick is a +25% buff over the original 20 FE/tick
    // so a single coal comfortably outpaces a single electric furnace's draw.
    private static final int STEAM_PER_TICK = 2;
    private static final int FE_PER_STEAM_TICK = 25;

    private final FluidTank steamTank = new io.github.ash1688.nuclearpowered.compat.gtceu.SteamTank(STEAM_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) { return 0; }

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
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private int lastFEGenerated = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> steamTank.getFluidAmount();
                case 1 -> steamTank.getCapacity();
                case 2 -> storedFE;
                case 3 -> ENERGY_CAPACITY;
                case 4 -> lastFEGenerated;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 5; }
    };

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.steam_engine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SteamEngineMenu(id, inv, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
        lazyFluidHandler = LazyOptional.of(() -> steamTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag tankTag = new CompoundTag();
        steamTank.writeToNBT(tankTag);
        tag.put("steam", tankTag);
        tag.putInt("fe", storedFE);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("steam"));
        storedFE = tag.getInt("fe");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false;
        lastFEGenerated = 0;

        // Convert steam to FE while both sides have capacity.
        if (steamTank.getFluidAmount() >= STEAM_PER_TICK
                && storedFE + FE_PER_STEAM_TICK <= ENERGY_CAPACITY) {
            steamTank.drain(STEAM_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            storedFE += FE_PER_STEAM_TICK;
            lastFEGenerated = FE_PER_STEAM_TICK;
            changed = true;
        }

        // Push FE to adjacent consumers.
        if (storedFE > 0) {
            for (Direction dir : Direction.values()) {
                if (storedFE <= 0) break;
                BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
                if (neighbour == null) continue;
                int delta = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(sink -> {
                    if (!sink.canReceive()) return 0;
                    int offered = Math.min(storedFE, MAX_OUTPUT_FE_PER_TICK);
                    return sink.receiveEnergy(offered, false);
                }).orElse(0);
                if (delta > 0) {
                    storedFE -= delta;
                    changed = true;
                }
            }
        }

        if (changed) setChanged(level, pos, state);
    }

    public FluidTank getSteamTank() { return steamTank; }
}
