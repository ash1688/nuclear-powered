package io.github.ash1688.nuclearpowered.block.coolingpond;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

// Cooling Pond — single-slot passive machine. Accepts a hot_spent_fuel_rod on
// any face, cools it for 60 s, then pushes the resulting
// depleted_uranium_fuel_rod out via auto-output. No GUI; line up as many pond
// blocks in a row as you want capacity.
public class CoolingPondBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    public static final int SLOT_INPUT = 0;
    // The right-hand slot now holds only the finished depleted rod. The hot rod
    // stays in SLOT_INPUT during cooling so the GUI clearly shows what's being
    // processed; only at the end of the cycle does a depleted rod appear here.
    // Constant name kept for compat with older saves and external references.
    public static final int SLOT_COOLING = 1;
    public static final int SLOT_OUTPUT = SLOT_COOLING;
    public static final int COOL_TICKS = 1200; // 60 seconds @ 20 TPS

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public int getSlotLimit(int slot) {
            // Input slot buffers a full stack of waiting hot rods; cooling slot
            // processes one at a time.
            return slot == SLOT_COOLING ? 1 : 64;
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) return stack.is(ModItems.HOT_SPENT_FUEL_ROD.get());
            return stack.is(ModItems.HOT_SPENT_FUEL_ROD.get())
                    || stack.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get());
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();
    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int coolProgress = 0;

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public CoolingPondBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLING_POND.get(), pos, state);
    }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        IItemTransfer machineItems = ItemTransferHelperImpl.toItemTransfer(itemHandler);

        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.cooling_pond");

        ui.mainGroup.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 20,
                () -> level != null && isInValidRow(level, worldPosition)
                        ? "§aMultiblock OK"
                        : "§cNeeds 1×3 row of ponds")
                .setDropShadow(true));

        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_INPUT, 56, 35, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_OUTPUT, 116, 35, true, false));
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(78, 41, 24,
                () -> coolProgress, () -> COOL_TICKS));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        return ui;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyExternalHandler = LazyOptional.of(() -> externalHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("cool", coolProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        // Save-format migration: the earliest Cooling Pond shipped as a single-
        // slot handler. deserializeNBT resizes to whatever Size is in the tag,
        // so old saves come back here with 1 slot and immediately crash in
        // tick() on the SLOT_COOLING (=1) read. Restore the current 2-slot
        // shape, preserving whatever was in the old slot 0.
        if (itemHandler.getSlots() < 2) {
            ItemStack migrated = itemHandler.getSlots() > 0
                    ? itemHandler.getStackInSlot(0)
                    : ItemStack.EMPTY;
            itemHandler.setSize(2);
            if (!migrated.isEmpty()) {
                // Old single-slot BEs held the rod that was actively cooling.
                // Under the current model the rod cools from SLOT_INPUT, so put
                // it there if it's still hot; if it had already cooled to a
                // depleted rod, drop it straight in the output slot.
                if (migrated.is(ModItems.HOT_SPENT_FUEL_ROD.get())) {
                    itemHandler.setStackInSlot(SLOT_INPUT, migrated);
                } else {
                    itemHandler.setStackInSlot(SLOT_OUTPUT, migrated);
                }
            }
        }
        coolProgress = tag.getInt("cool");
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) inv.setItem(i, itemHandler.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (!isInValidRow(level, pos)) return;
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        // Cool the hot rod in-place: it stays visible in SLOT_INPUT while the
        // bar fills, and only converts to a depleted rod (in the output slot)
        // once the cycle completes. Pauses if the output slot is full.
        boolean canRun = input.is(ModItems.HOT_SPENT_FUEL_ROD.get()) && canFitDepleted(output);
        if (canRun) {
            coolProgress++;
            setChanged();
            if (coolProgress >= COOL_TICKS) {
                input.shrink(1);
                ItemStack newOutput = itemHandler.getStackInSlot(SLOT_OUTPUT);
                if (newOutput.isEmpty()) {
                    itemHandler.setStackInSlot(SLOT_OUTPUT,
                            new ItemStack(ModItems.DEPLETED_URANIUM_FUEL_ROD.get()));
                } else {
                    newOutput.grow(1);
                }
                coolProgress = 0;
            }
        } else if (coolProgress != 0) {
            // No fuel or output blocked — reset progress so it doesn't carry
            // over into the next rod mid-stack.
            coolProgress = 0;
            setChanged();
        }

        if (!itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            autoPushOut(level, pos);
        }
    }

    private boolean canFitDepleted(ItemStack output) {
        if (output.isEmpty()) return true;
        if (!output.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    // Multiblock gate: a pond only operates when it belongs to a contiguous
    // run of 3+ cooling ponds along the X or Z axis (any position in the row).
    private boolean isInValidRow(Level level, BlockPos pos) {
        return countAxis(level, pos, Direction.Axis.X) >= 3
                || countAxis(level, pos, Direction.Axis.Z) >= 3;
    }

    private int countAxis(Level level, BlockPos pos, Direction.Axis axis) {
        Direction pos1 = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        return 1 + countDir(level, pos, pos1) + countDir(level, pos, neg);
    }

    private int countDir(Level level, BlockPos pos, Direction dir) {
        int count = 0;
        BlockPos cursor = pos.relative(dir);
        while (level.getBlockState(cursor).getBlock() instanceof CoolingPondBlock) {
            count++;
            cursor = cursor.relative(dir);
        }
        return count;
    }

    private void autoPushOut(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            ItemStack source = itemHandler.getStackInSlot(SLOT_COOLING);
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

    // External wrapper — hoppers/pipes insert hot rods into the queue (SLOT_INPUT)
    // and extract only cooled depleted rods out of SLOT_COOLING.
    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOT_INPUT) return stack;
            if (!stack.is(ModItems.HOT_SPENT_FUEL_ROD.get())) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_COOLING) return ItemStack.EMPTY;
            if (!itemHandler.getStackInSlot(slot).is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
