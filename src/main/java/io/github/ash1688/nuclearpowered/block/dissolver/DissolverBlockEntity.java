package io.github.ash1688.nuclearpowered.block.dissolver;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import io.github.ash1688.nuclearpowered.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

// Tier 1 reprocessing — Step 2 (PUREX dissolution). Chopped fuel + nitric acid
// -> dissolved fuel solution (item) + insoluble reactor sludge. Nitric acid can
// be pumped in via a fluid pipe or poured in from a bucket slot (leaving an
// empty bucket that auto-outputs). FE cost 150/tick.
public class DissolverBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_FUEL = 1;
    public static final int SLOT_OUTPUT_SLUDGE = 2;
    public static final int SLOT_BUCKET = 3;

    public static final int TANK_CAPACITY_MB = 4000;
    private static final int BUCKET_VOLUME_MB = 1000;
    private static final int ACID_PER_BATCH = 250;

    public static final int ENERGY_CAPACITY = 20_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 512;
    public static final int FE_PER_TICK = 150;
    public static final int PROCESS_TICKS = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) return stack.is(ModItems.CHOPPED_FUEL.get());
            if (slot == SLOT_BUCKET) return stack.is(ModItems.NITRIC_ACID_BUCKET.get());
            return false;
        }
    };

    // Single-slot upgrade bay. Accepts only the Dissolver Reagent Saver.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.DISSOLVER_REAGENT_SAVER.get());
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();

    private final FluidTank acidTank = new FluidTank(TANK_CAPACITY_MB,
            stack -> stack.getFluid() == ModFluids.NITRIC_ACID.get()) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

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

    public DissolverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISSOLVER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }
    public IItemHandler getUpgradeHandlerForMenu() { return upgradeHandler; }
    private boolean hasReagentSaver() {
        return upgradeHandler.getStackInSlot(0).is(ModItems.DISSOLVER_REAGENT_SAVER.get());
    }
    private int effectiveAcidPerBatch() {
        return hasReagentSaver() ? ACID_PER_BATCH / 2 : ACID_PER_BATCH;
    }
    public FluidTank getAcidTank() { return acidTank; }
    public boolean isAutoInput() { return autoInput; }
    public boolean isAutoOutput() { return autoOutput; }
    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }
    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(176, 166, this, player);
        IItemTransfer machineItems = ItemTransferHelperImpl.toItemTransfer(itemHandler);
        IItemTransfer upgradeItems = ItemTransferHelperImpl.toItemTransfer(upgradeHandler);

        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.dissolver");

        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_INPUT, 38, 35, true, true));
        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_BUCKET, 38, 17, true, true));
        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_OUTPUT_FUEL, 96, 26, true, false));
        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_OUTPUT_SLUDGE, 96, 44, true, false));
        ui.mainGroup.addWidget(new SlotWidget(upgradeItems, 0, 134, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(60, 41, 24,
                () -> progress, () -> PROCESS_TICKS));

        ui.mainGroup.addWidget(NPMachineUI.tankBar(116, 17, acidTank));
        ui.mainGroup.addWidget(NPMachineUI.feBar(152, 17,
                () -> storedFE, ENERGY_CAPACITY));

        ui.mainGroup.addWidget(NPMachineUI.toggleButton(8, 58, 64, "Auto In",
                () -> autoInput, this::toggleAutoInput));
        ui.mainGroup.addWidget(NPMachineUI.toggleButton(80, 58, 64, "Auto Out",
                () -> autoOutput, this::toggleAutoOutput));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        return ui;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyExternalHandler = LazyOptional.of(() -> externalHandler);
        lazyFluidHandler = LazyOptional.of(() -> acidTank);
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyEnergy.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.put("upgrade", upgradeHandler.serializeNBT());
        CompoundTag tankTag = new CompoundTag();
        acidTank.writeToNBT(tankTag);
        tag.put("acid", tankTag);
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
        acidTank.readFromNBT(tag.getCompound("acid"));
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
        // Drain a nitric acid bucket into the tank if there's room.
        ItemStack bucket = itemHandler.getStackInSlot(SLOT_BUCKET);
        if (bucket.is(ModItems.NITRIC_ACID_BUCKET.get())
                && acidTank.getFluidAmount() + BUCKET_VOLUME_MB <= acidTank.getCapacity()) {
            acidTank.fill(new FluidStack(ModFluids.NITRIC_ACID.get(), BUCKET_VOLUME_MB),
                    IFluidHandler.FluidAction.EXECUTE);
            itemHandler.setStackInSlot(SLOT_BUCKET, new ItemStack(Items.BUCKET));
            setChanged(level, pos, state);
        }

        boolean dissolvingThisTick = false;
        if (canDissolve()) {
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= PROCESS_TICKS) {
                    doDissolve();
                    progress = 0;
                }
            }
            dissolvingThisTick = true;
        }
        if (!dissolvingThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }

        if (autoOutput) {
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_FUEL).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_FUEL);
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_SLUDGE).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_SLUDGE);
            // Empty buckets auto-push too so full-cycle acid feeding can be automated.
            if (itemHandler.getStackInSlot(SLOT_BUCKET).is(Items.BUCKET)) autoPushSlot(level, pos, SLOT_BUCKET);
        }
    }

    private boolean canDissolve() {
        if (!itemHandler.getStackInSlot(SLOT_INPUT).is(ModItems.CHOPPED_FUEL.get())) return false;
        if (acidTank.getFluidAmount() < effectiveAcidPerBatch()) return false;
        return canFit(SLOT_OUTPUT_FUEL, new ItemStack(ModItems.DISSOLVED_FUEL.get()))
                && canFit(SLOT_OUTPUT_SLUDGE, new ItemStack(ModItems.REACTOR_SLUDGE.get()));
    }

    private boolean canFit(int slot, ItemStack result) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void doDissolve() {
        itemHandler.getStackInSlot(SLOT_INPUT).shrink(1);
        acidTank.drain(effectiveAcidPerBatch(), IFluidHandler.FluidAction.EXECUTE);
        addTo(SLOT_OUTPUT_FUEL, new ItemStack(ModItems.DISSOLVED_FUEL.get()));
        addTo(SLOT_OUTPUT_SLUDGE, new ItemStack(ModItems.REACTOR_SLUDGE.get()));
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
            if (!autoInput) return stack;
            if (slot != SLOT_INPUT && slot != SLOT_BUCKET) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput) return ItemStack.EMPTY;
            if (slot != SLOT_OUTPUT_FUEL && slot != SLOT_OUTPUT_SLUDGE && slot != SLOT_BUCKET) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
