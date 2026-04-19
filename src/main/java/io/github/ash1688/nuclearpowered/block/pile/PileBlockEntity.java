package io.github.ash1688.nuclearpowered.block.pile;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.menu.PileMenu;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class PileBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_DEPLETED = 1;

    // Burn duration per fuel rod (ticks). ~200s / 3.3 min per rod.
    public static final int BURN_TICKS_PER_ROD = 4000;

    public static final int MAX_HEAT = 1000;
    private static final int HEAT_RISE_PER_TICK = 5;
    private static final int HEAT_DECAY_PER_TICK = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_FUEL) return stack.is(ModItems.URANIUM_FUEL_ROD.get());
            // Depleted slot: nothing accepts inserts from player — the pile produces into it.
            return super.isItemValid(slot, stack);
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int heat = 0;
    private int burnTime = 0;           // ticks remaining on the current rod (0 = not burning)
    private boolean autoInput = true;
    private boolean autoOutput = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> MAX_HEAT;
                case 2 -> burnTime;
                case 3 -> BURN_TICKS_PER_ROD;
                case 4 -> autoInput ? 1 : 0;
                case 5 -> autoOutput ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> heat = value;
                case 2 -> burnTime = value;
                case 4 -> autoInput = value != 0;
                case 5 -> autoOutput = value != 0;
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    public PileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAPHITE_PILE.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public int getHeat() { return heat; }

    public boolean isBurning() { return burnTime > 0; }

    public boolean isAutoInput() { return autoInput; }

    public boolean isAutoOutput() { return autoOutput; }

    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }

    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.graphite_pile");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PileMenu(id, inv, this, data);
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
        tag.putInt("heat", heat);
        tag.putInt("burnTime", burnTime);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        heat = tag.getInt("heat");
        burnTime = tag.getInt("burnTime");
        autoInput = !tag.contains("autoInput") || tag.getBoolean("autoInput");
        autoOutput = !tag.contains("autoOutput") || tag.getBoolean("autoOutput");
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        if (burnTime > 0) {
            // Active burn: heat rises, fuel consumed.
            burnTime--;
            if (heat < MAX_HEAT) {
                heat = Math.min(MAX_HEAT, heat + HEAT_RISE_PER_TICK);
            }
            if (burnTime == 0) {
                produceDepletedRod();
            }
            changed = true;
        } else {
            // Idle: try to start burning the next rod if the depleted slot can take another.
            if (canStartBurn()) {
                itemHandler.getStackInSlot(SLOT_FUEL).shrink(1);
                burnTime = BURN_TICKS_PER_ROD;
                changed = true;
            }
            if (heat > 0) {
                heat = Math.max(0, heat - HEAT_DECAY_PER_TICK);
                changed = true;
            }
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private boolean canStartBurn() {
        if (itemHandler.getStackInSlot(SLOT_FUEL).isEmpty()) return false;
        if (!itemHandler.getStackInSlot(SLOT_FUEL).is(ModItems.URANIUM_FUEL_ROD.get())) return false;
        ItemStack depleted = itemHandler.getStackInSlot(SLOT_DEPLETED);
        if (depleted.isEmpty()) return true;
        if (!depleted.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())) return false;
        return depleted.getCount() + 1 <= depleted.getMaxStackSize();
    }

    private void produceDepletedRod() {
        ItemStack depleted = itemHandler.getStackInSlot(SLOT_DEPLETED);
        if (depleted.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_DEPLETED, new ItemStack(ModItems.DEPLETED_URANIUM_FUEL_ROD.get()));
        } else {
            depleted.grow(1);
        }
    }

    // External hoppers/pipes: fuel rods insert into the fuel slot when autoInput is on;
    // depleted rods extract from the depleted slot when autoOutput is on.
    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput || slot != SLOT_FUEL) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput || slot != SLOT_DEPLETED) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
