package io.github.ash1688.nuclearpowered.block.cscolumn;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableEUBlock;
import io.github.ash1688.nuclearpowered.energy.EnergyMode;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
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
import java.util.Random;

// Tier 1 reprocessing — Step 4. Cesium column: fission product stream runs
// through a consumable ion exchange resin, pulling out Cs-137 and leaving a
// residual waste stream headed for vitrification. 350 FE/tick.
public class CsColumnBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int SLOT_INPUT_STREAM = 0;
    public static final int SLOT_INPUT_RESIN = 1;
    public static final int SLOT_OUTPUT_CS = 2;
    public static final int SLOT_OUTPUT_WASTE = 3;

    public static final int ENERGY_CAPACITY = 40_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 512;
    public static final int FE_PER_TICK = 350;
    public static final int PROCESS_TICKS = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT_STREAM) return stack.is(ModItems.FISSION_PRODUCT_STREAM.get());
            if (slot == SLOT_INPUT_RESIN) return stack.is(ModItems.ION_EXCHANGE_RESIN.get());
            return false;
        }
    };

    // Single-slot upgrade bay. Accepts only the Cs Resin Saver.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.CS_RESIN_SAVER.get());
        }
    };

    // Random source for the 50% resin-save coin flip. Reuses a shared instance
    // — resin conservation doesn't need cryptographic entropy.
    private static final Random RESIN_RNG = new Random();

    private final IItemHandler externalHandler = new SidedItemHandler();
    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int progress = 0;
    private int storedFE = 0;

    // Dual-energy mode flag. FE is the cross-mod default; EU only does
    // anything when GTCEU is loaded and the player has toggled. The toggle
    // is gated on progress == 0 so a mid-process switch can't lose work.
    private EnergyMode energyMode = EnergyMode.FE;

    /** Set true by load() when the BE was placed fresh (no NBT tag yet)
     *  so onLoad() can scan adjacent blocks and adopt an EU cable's mode
     *  automatically. Avoids forcing the player to toggle the UI when the
     *  cable type already disambiguates. */
    private transient boolean pendingAutoDetect = false;
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

    /** EU consumer wrapper for the FE buffer; populated in onLoad when
     *  GT is present. Exposed by getCapability when energyMode == EU so
     *  EU cables / GT producers can connect and push. */
    @SuppressWarnings("rawtypes")
    private LazyOptional lazyEUConsumer = LazyOptional.empty();

    public CsColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CS_COLUMN.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }
    public IItemHandler getUpgradeHandlerForMenu() { return upgradeHandler; }
    private boolean hasResinSaver() {
        return upgradeHandler.getStackInSlot(0).is(ModItems.CS_RESIN_SAVER.get());
    }
    public boolean isAutoInput() { return autoInput; }
    public boolean isAutoOutput() { return autoOutput; }
    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }
    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    public EnergyMode getEnergyMode() {
        return energyMode;
    }

    /** True if a mode switch would be valid right now (idle + EU available). */
    public boolean canToggleEnergyMode() {
        if (progress > 0) return false;
        if (energyMode == EnergyMode.FE && !GTCompat.isLoaded()) return false;
        return true;
    }

    public void toggleEnergyMode() {
        if (!canToggleEnergyMode()) return;
        energyMode = energyMode.opposite();
        lazyEnergy.invalidate();
        lazyEUConsumer.invalidate();
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
        if (GTCompat.isLoaded()) {
            lazyEUConsumer = GTEnergyCompat.wrapFEAsEUConsumer(externalEnergy);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        IItemTransfer machineItems = ItemTransferHelperImpl.toItemTransfer(itemHandler);
        IItemTransfer upgradeItems = ItemTransferHelperImpl.toItemTransfer(upgradeHandler);

        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.cs_column");

        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_INPUT_STREAM, 38, 26, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_INPUT_RESIN, 38, 44, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_OUTPUT_CS, 108, 26, true, false));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_OUTPUT_WASTE, 108, 44, true, false));
        ui.mainGroup.addWidget(NPMachineUI.slot(upgradeItems, 0, 134, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(78, 41, 24,
                () -> progress, () -> PROCESS_TICKS));

        ui.mainGroup.addWidget(NPMachineUI.feBar(152, 17,
                () -> storedFE, ENERGY_CAPACITY,
                () -> energyMode.displayUnit()));

        // Energy-mode tag above the FE bar — green for FE, light blue for EU.
        ui.mainGroup.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 152, 8,
                () -> energyMode == EnergyMode.FE ? "§aFE" : "§bEU")
                .setDropShadow(true));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);

        ui.mainGroup.addWidget(new io.github.ash1688.nuclearpowered.client.ui.NPTabs()
                .ioTab(() -> autoInput, this::toggleAutoInput,
                        () -> autoOutput, this::toggleAutoOutput)
                .energyTab(this::getEnergyMode, this::toggleEnergyMode,
                        this::canToggleEnergyMode)
                .build());
        return ui;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        if (cap == ForgeCapabilities.ENERGY && energyMode == EnergyMode.FE) {
            return lazyEnergy.cast();
        }
        if (energyMode == EnergyMode.EU && GTCompat.isLoaded()
                && GTEnergyCompat.isEnergyContainerCap(cap)) {
            return (LazyOptional<T>) lazyEUConsumer;
        }
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
        lazyEUConsumer.invalidate();
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
        tag.putString("energyMode", energyMode.name());
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
        if (tag.contains("energyMode")) {
            try { energyMode = EnergyMode.valueOf(tag.getString("energyMode")); }
            catch (IllegalArgumentException ignored) { energyMode = EnergyMode.FE; }
        }
        else {
            pendingAutoDetect = true;
        }
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots() + upgradeHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) inv.setItem(i, itemHandler.getStackInSlot(i));
        inv.setItem(itemHandler.getSlots(), upgradeHandler.getStackInSlot(0));
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean runningThisTick = false;
        if (canRun()) {
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= PROCESS_TICKS) { doRun(); progress = 0; }
            }
            runningThisTick = true;
        }
        if (!runningThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }

        if (autoOutput) {
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_CS).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_CS);
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT_WASTE).isEmpty()) autoPushSlot(level, pos, SLOT_OUTPUT_WASTE);
        }
    }

    private boolean canRun() {
        if (!itemHandler.getStackInSlot(SLOT_INPUT_STREAM).is(ModItems.FISSION_PRODUCT_STREAM.get())) return false;
        if (!itemHandler.getStackInSlot(SLOT_INPUT_RESIN).is(ModItems.ION_EXCHANGE_RESIN.get())) return false;
        return canFit(SLOT_OUTPUT_CS, new ItemStack(ModItems.CESIUM_137.get()))
                && canFit(SLOT_OUTPUT_WASTE, new ItemStack(ModItems.RESIDUAL_WASTE.get()));
    }

    private boolean canFit(int slot, ItemStack result) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void doRun() {
        itemHandler.getStackInSlot(SLOT_INPUT_STREAM).shrink(1);
        // Cs Resin Saver: 50 % of batches don't consume the resin.
        if (!hasResinSaver() || RESIN_RNG.nextBoolean()) {
            itemHandler.getStackInSlot(SLOT_INPUT_RESIN).shrink(1);
        }
        addTo(SLOT_OUTPUT_CS, new ItemStack(ModItems.CESIUM_137.get()));
        addTo(SLOT_OUTPUT_WASTE, new ItemStack(ModItems.RESIDUAL_WASTE.get()));
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
            if (slot != SLOT_INPUT_STREAM && slot != SLOT_INPUT_RESIN) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput) return ItemStack.EMPTY;
            if (slot != SLOT_OUTPUT_CS && slot != SLOT_OUTPUT_WASTE) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
