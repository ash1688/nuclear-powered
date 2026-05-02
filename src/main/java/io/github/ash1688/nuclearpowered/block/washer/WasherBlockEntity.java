package io.github.ash1688.nuclearpowered.block.washer;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.energy.EnergyMode;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import io.github.ash1688.nuclearpowered.recipe.WasherRecipe;
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
import net.minecraft.world.level.material.Fluids;
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
import java.util.Optional;

public class WasherBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BUCKET = 2;

    public static final int TANK_CAPACITY_MB = 4000;
    private static final int BUCKET_VOLUME_MB = 1000;

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 256;
    public static final int FE_PER_TICK = 15;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_BUCKET) return stack.is(Items.WATER_BUCKET);
            return super.isItemValid(slot, stack);
        }
    };

    // Single-slot upgrade bay. Accepts only the Washer Speed Card.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(io.github.ash1688.nuclearpowered.init.ModItems.WASHER_SPEED_CARD.get());
        }
    };

    // External wrapper gating item handler access by auto I/O toggles and slot direction.
    private final IItemHandler externalHandler = new SidedItemHandler();

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY_MB,
            stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    /** EU consumer wrapper for the FE buffer; populated in onLoad when
     *  GT is present. Exposed by getCapability when energyMode == EU so
     *  EU cables / GT producers can connect and push. */
    @SuppressWarnings("rawtypes")
    private LazyOptional lazyEUConsumer = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = WasherRecipe.DEFAULT_PROCESSING_TIME;
    private boolean autoInput = true;
    private boolean autoOutput = true;
    private int storedFE = 0;

    // Dual-energy mode flag. FE is the cross-mod default; EU only does
    // anything when GTCEU is loaded and the player has toggled. The toggle
    // is gated on progress == 0 so a mid-process switch can't lose work.
    private EnergyMode energyMode = EnergyMode.FE;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int canAccept = Math.min(ENERGY_CAPACITY - storedFE, Math.min(amount, ENERGY_MAX_INPUT_PER_TICK));
            if (canAccept <= 0) return 0;
            if (!simulate) {
                storedFE += canAccept;
                setChanged();
            }
            return canAccept;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public WasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public IItemHandler getUpgradeHandlerForMenu() { return upgradeHandler; }

    private boolean hasSpeedUpgrade() {
        return upgradeHandler.getStackInSlot(0)
                .is(io.github.ash1688.nuclearpowered.init.ModItems.WASHER_SPEED_CARD.get());
    }

    public FluidTank getWaterTank() { return waterTank; }

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
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.washer");

        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_INPUT, 38, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_OUTPUT, 96, 35, true, false));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_BUCKET, 38, 17, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(upgradeItems, 0, 134, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(60, 41, 24,
                () -> progress, () -> maxProgress));

        ui.mainGroup.addWidget(NPMachineUI.tankBar(116, 17, waterTank));
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
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
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
        lazyFluidHandler = LazyOptional.of(() -> waterTank);
        lazyEnergy = LazyOptional.of(() -> externalEnergy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyEnergy.invalidate();
        lazyEUConsumer.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.put("upgrade", upgradeHandler.serializeNBT());
        CompoundTag tankTag = new CompoundTag();
        waterTank.writeToNBT(tankTag);
        tag.put("water", tankTag);
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
        waterTank.readFromNBT(tag.getCompound("water"));
        progress = tag.getInt("progress");
        storedFE = tag.getInt("fe");
        autoInput = !tag.contains("autoInput") || tag.getBoolean("autoInput");
        autoOutput = !tag.contains("autoOutput") || tag.getBoolean("autoOutput");
        if (tag.contains("energyMode")) {
            try { energyMode = EnergyMode.valueOf(tag.getString("energyMode")); }
            catch (IllegalArgumentException ignored) { energyMode = EnergyMode.FE; }
        }
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots() + upgradeHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        inv.setItem(itemHandler.getSlots(), upgradeHandler.getStackInSlot(0));
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        ItemStack bucket = itemHandler.getStackInSlot(SLOT_BUCKET);
        if (bucket.is(Items.WATER_BUCKET)
                && waterTank.getFluidAmount() + BUCKET_VOLUME_MB <= waterTank.getCapacity()) {
            waterTank.fill(new FluidStack(Fluids.WATER, BUCKET_VOLUME_MB), IFluidHandler.FluidAction.EXECUTE);
            itemHandler.setStackInSlot(SLOT_BUCKET, new ItemStack(Items.BUCKET));
            setChanged(level, pos, state);
        }

        Optional<WasherRecipe> recipe = findMatchingRecipe(level);
        boolean washingThisTick = false;
        if (recipe.isPresent() && canFit(recipe.get().getResult()) && hasEnoughFluid(recipe.get().getFluid())) {
            int base = recipe.get().getProcessingTime();
            maxProgress = hasSpeedUpgrade() ? Math.max(1, base / 2) : base;
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= maxProgress) {
                    craft(recipe.get());
                    progress = 0;
                }
            }
            washingThisTick = true;
        }
        if (!washingThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }

        if (autoOutput) {
            if (!itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
                autoPushSlot(level, pos, SLOT_OUTPUT);
            }
            // Empty buckets in the bucket slot also auto-push out for full-cycle water supply.
            ItemStack bucketSlot = itemHandler.getStackInSlot(SLOT_BUCKET);
            if (bucketSlot.is(Items.BUCKET)) {
                autoPushSlot(level, pos, SLOT_BUCKET);
            }
        }
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
            if (moved > 0) {
                source.shrink(moved);
                setChanged();
            }
        }
    }

    private Optional<WasherRecipe> findMatchingRecipe(Level level) {
        if (itemHandler.getStackInSlot(SLOT_INPUT).isEmpty()) return Optional.empty();
        SimpleContainer probe = new SimpleContainer(1);
        probe.setItem(0, itemHandler.getStackInSlot(SLOT_INPUT));
        return level.getRecipeManager().getRecipeFor(ModRecipes.WASHING_TYPE.get(), probe, level);
    }

    private boolean canFit(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean hasEnoughFluid(FluidStack required) {
        FluidStack current = waterTank.getFluid();
        if (current.isEmpty()) return false;
        if (current.getFluid() != required.getFluid()) return false;
        return current.getAmount() >= required.getAmount();
    }

    private void craft(WasherRecipe recipe) {
        ItemStack result = recipe.getResult();
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        itemHandler.getStackInSlot(SLOT_INPUT).shrink(1);
        waterTank.drain(recipe.getFluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
    }

    // External hoppers/pipes see only this wrapper: inserts allowed to INPUT and BUCKET
    // slots when autoInput is on; extracts allowed from OUTPUT and BUCKET slots when
    // autoOutput is on. The BUCKET slot is both-direction so water buckets flow in and
    // empty buckets flow back out.
    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput) return stack;
            if (slot != SLOT_INPUT && slot != SLOT_BUCKET) return stack;
            if (slot == SLOT_INPUT && !hasWashingRecipe(stack)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput) return ItemStack.EMPTY;
            if (slot != SLOT_OUTPUT && slot != SLOT_BUCKET) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT && !hasWashingRecipe(stack)) return false;
            return itemHandler.isItemValid(slot, stack);
        }

        private boolean hasWashingRecipe(ItemStack stack) {
            if (level == null || stack.isEmpty()) return false;
            SimpleContainer probe = new SimpleContainer(1);
            probe.setItem(0, stack);
            return level.getRecipeManager()
                    .getRecipeFor(ModRecipes.WASHING_TYPE.get(), probe, level)
                    .isPresent();
        }
    }
}
