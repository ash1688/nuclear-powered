package io.github.ash1688.nuclearpowered.block.crusher;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import io.github.ash1688.nuclearpowered.recipe.CrusherRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Containers;
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
import java.util.Optional;

public class CrusherBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int MAX_PROGRESS = CrusherRecipe.DEFAULT_PROCESSING_TIME;

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 256;
    public static final int FE_PER_TICK = 15;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Single-slot upgrade bay. Accepts only the Crusher Speed Card. Exposed to
    // the menu (not to pipes/hoppers) so players swap cards manually.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(io.github.ash1688.nuclearpowered.init.ModItems.CRUSHER_SPEED_CARD.get());
        }
    };

    // Wraps the raw handler for external (hopper/pipe) access only — obeys the auto in/out
    // toggles and restricts inserts to the input slot and extracts to the output slot.
    // The menu uses the raw handler directly so the player can always click items in/out.
    private final IItemHandler externalHandler = new SidedItemHandler();

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = MAX_PROGRESS;
    private boolean autoInput = true;
    private boolean autoOutput = true;
    private int storedFE = 0;

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

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    // ContainerData is gone — LDLib widgets read field state directly via
    // DoubleSupplier lambdas in createUI, and LDLib handles the server->client
    // sync internally. No vanilla AbstractContainerMenu means no need for
    // a serialised data slot bridge.

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() {
        return itemHandler;
    }

    public IItemHandler getUpgradeHandlerForMenu() {
        return upgradeHandler;
    }

    private boolean hasSpeedUpgrade() {
        return upgradeHandler.getStackInSlot(0)
                .is(io.github.ash1688.nuclearpowered.init.ModItems.CRUSHER_SPEED_CARD.get());
    }

    public boolean isAutoInput() {
        return autoInput;
    }

    public boolean isAutoOutput() {
        return autoOutput;
    }

    public void toggleAutoInput() {
        autoInput = !autoInput;
        setChanged();
    }

    public void toggleAutoOutput() {
        autoOutput = !autoOutput;
        setChanged();
    }

    // --- LDLib UI ---

    @Override
    public BlockEntity self() {
        return this;
    }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(176, 166, this, player);
        IItemTransfer machineItems = ItemTransferHelperImpl.toItemTransfer(itemHandler);
        IItemTransfer upgradeItems = ItemTransferHelperImpl.toItemTransfer(upgradeHandler);

        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.crusher");

        // Input -> progress arrow -> output, with upgrade bay to the right.
        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_INPUT, 56, 35, true, true));
        ui.mainGroup.addWidget(new SlotWidget(machineItems, SLOT_OUTPUT, 116, 35, true, false));
        ui.mainGroup.addWidget(new SlotWidget(upgradeItems, 0, 134, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(78, 41, 24,
                () -> progress, () -> maxProgress));

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
        // Default to true for worlds created before the toggles existed.
        autoInput = !tag.contains("autoInput") || tag.getBoolean("autoInput");
        autoOutput = !tag.contains("autoOutput") || tag.getBoolean("autoOutput");
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
        Optional<CrusherRecipe> recipe = findMatchingRecipe(level);
        boolean crushingThisTick = false;
        if (recipe.isPresent() && canFit(recipe.get().getResult())) {
            int base = recipe.get().getProcessingTime();
            maxProgress = hasSpeedUpgrade() ? Math.max(1, base / 2) : base;
            // Progress pauses (doesn't reset) when power runs dry, matching the
            // electric furnace so a starved crusher resumes where it left off.
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= maxProgress) {
                    craft(recipe.get());
                    progress = 0;
                }
            }
            crushingThisTick = true;
        }
        if (!crushingThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }

        if (autoOutput && !itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            autoPushSlot(level, pos, SLOT_OUTPUT);
        }
    }

    // Active push of an output-slot stack into any adjacent block that exposes an
    // IItemHandler capability. Receiving block decides what slots will accept — its
    // own isItemValid / sided-filter logic still applies, so a downstream machine
    // with "Auto In: OFF" will reject the incoming item.
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

    private Optional<CrusherRecipe> findMatchingRecipe(Level level) {
        if (itemHandler.getStackInSlot(SLOT_INPUT).isEmpty()) return Optional.empty();
        SimpleContainer probe = new SimpleContainer(1);
        probe.setItem(0, itemHandler.getStackInSlot(SLOT_INPUT));
        return level.getRecipeManager().getRecipeFor(ModRecipes.CRUSHING_TYPE.get(), probe, level);
    }

    private boolean canFit(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(CrusherRecipe recipe) {
        ItemStack result = recipe.getResult();
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        itemHandler.getStackInSlot(SLOT_INPUT).shrink(1);
    }

    // Gates external item-handler access through the auto in/out master toggles and restricts
    // external writes to the input slot / external reads to the output slot. Pipes and hoppers
    // see this wrapper; the in-game slot GUI sees the raw handler.
    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput || slot != SLOT_INPUT) return stack;
            if (!hasCrushingRecipe(stack)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput || slot != SLOT_OUTPUT) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT && !hasCrushingRecipe(stack)) return false;
            return itemHandler.isItemValid(slot, stack);
        }

        private boolean hasCrushingRecipe(ItemStack stack) {
            if (level == null || stack.isEmpty()) return false;
            SimpleContainer probe = new SimpleContainer(1);
            probe.setItem(0, stack);
            return level.getRecipeManager()
                    .getRecipeFor(ModRecipes.CRUSHING_TYPE.get(), probe, level)
                    .isPresent();
        }
    }
}
