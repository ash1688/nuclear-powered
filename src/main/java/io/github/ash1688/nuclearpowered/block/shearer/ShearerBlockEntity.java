package io.github.ash1688.nuclearpowered.block.shearer;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.menu.ShearerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

// Tier 1 reprocessing — Step 1. Mechanically shears depleted fuel rods into
// chopped fuel plus scrap cladding. Only operation currently: one depleted rod
// -> one chopped_fuel + one cladding_scrap. Power-hungry by design (50 FE/tick)
// to encourage players to build more than one pile for reprocessing throughput.
public class ShearerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_FUEL = 1;
    public static final int SLOT_OUTPUT_SCRAP = 2;

    // Tier-1 reprocessing chain is priced so all five machines running
    // together pull ~1200 FE/tick — above a single battery's 1024 FE/tick
    // output ceiling, so sustained throughput needs either two piles or
    // multiple batteries on the network. Power cost ramps along the chain:
    //   Shearer 50 -> Dissolver 150 -> Extraction 250 -> Cs 350 -> Vitrifier 400.
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 512;
    public static final int FE_PER_TICK = 50;
    public static final int PROCESS_TICKS = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) return stack.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get());
            return false;
        }
    };

    // Single-slot upgrade bay. Accepts only the Shearer Speed Card.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.SHEARER_SPEED_CARD.get());
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();
    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int progress = 0;
    private int storedFE = 0;
    private boolean autoInput = true;
    private boolean autoOutput = true;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int canAccept = Math.min(ENERGY_CAPACITY - storedFE, Math.min(amount, ENERGY_MAX_INPUT_PER_TICK));
            if (canAccept <= 0) return 0;
            if (!simulate) { storedFE += canAccept; setChanged(); }
            return canAccept;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> effectiveProcessTicks();
                case 2 -> autoInput ? 1 : 0;
                case 3 -> autoOutput ? 1 : 0;
                case 4 -> storedFE;
                case 5 -> ENERGY_CAPACITY;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> autoInput = value != 0;
                case 3 -> autoOutput = value != 0;
                case 4 -> storedFE = value;
            }
        }
        @Override public int getCount() { return 6; }
    };

    public ShearerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHEARER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }
    public IItemHandler getUpgradeHandlerForMenu() { return upgradeHandler; }
    private boolean hasSpeedUpgrade() {
        return upgradeHandler.getStackInSlot(0).is(ModItems.SHEARER_SPEED_CARD.get());
    }
    private int effectiveProcessTicks() {
        return hasSpeedUpgrade() ? Math.max(1, PROCESS_TICKS / 2) : PROCESS_TICKS;
    }
    public boolean isAutoInput() { return autoInput; }
    public boolean isAutoOutput() { return autoOutput; }
    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }
    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    @Override public Component getDisplayName() { return Component.translatable("block.nuclearpowered.shearer"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShearerMenu(id, inv, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyExternalHandler = LazyOptional.of(() -> externalHandler);
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
        lazyEnergy.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.put("upgrade", upgradeHandler.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("fe", storedFE);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("upgrade")) upgradeHandler.deserializeNBT(tag.getCompound("upgrade"));
        progress = tag.getInt("progress");
        storedFE = tag.getInt("fe");
        autoInput = !tag.contains("autoInput") || tag.getBoolean("autoInput");
        autoOutput = !tag.contains("autoOutput") || tag.getBoolean("autoOutput");
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots() + upgradeHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) inv.setItem(i, itemHandler.getStackInSlot(i));
        inv.setItem(itemHandler.getSlots(), upgradeHandler.getStackInSlot(0));
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean shearingThisTick = false;
        if (canShear()) {
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= effectiveProcessTicks()) {
                    doShear();
                    progress = 0;
                }
            }
            shearingThisTick = true;
        }
        if (!shearingThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }
        if (autoOutput) {
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_FUEL).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_FUEL);
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_SCRAP).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_SCRAP);
        }
    }

    private boolean canShear() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (!input.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())) return false;
        return canFit(SLOT_OUTPUT_FUEL, new ItemStack(ModItems.CHOPPED_FUEL.get()))
                && canFit(SLOT_OUTPUT_SCRAP, new ItemStack(ModItems.CLADDING_SCRAP.get()));
    }

    private boolean canFit(int slot, ItemStack result) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void doShear() {
        itemHandler.getStackInSlot(SLOT_INPUT).shrink(1);
        addTo(SLOT_OUTPUT_FUEL, new ItemStack(ModItems.CHOPPED_FUEL.get()));
        addTo(SLOT_OUTPUT_SCRAP, new ItemStack(ModItems.CLADDING_SCRAP.get()));
    }

    private void addTo(int slot, ItemStack result) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) itemHandler.setStackInSlot(slot, result);
        else current.grow(result.getCount());
    }

    private void autoPushSlot(Level level, BlockPos pos, int slot) {
        for (Direction dir : Direction.values()) {
            ItemStack source = itemHandler.getStackInSlot(slot);
            if (source.isEmpty()) return;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            IItemHandler sink = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (sink == null) continue;
            ItemStack attempt = source.copy();
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(sink, attempt, false);
            int moved = attempt.getCount() - remaining.getCount();
            if (moved > 0) { source.shrink(moved); setChanged(); }
        }
    }

    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput || slot != SLOT_INPUT) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput) return ItemStack.EMPTY;
            if (slot != SLOT_OUTPUT_FUEL && slot != SLOT_OUTPUT_SCRAP) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
