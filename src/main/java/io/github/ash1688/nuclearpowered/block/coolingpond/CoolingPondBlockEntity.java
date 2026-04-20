package io.github.ash1688.nuclearpowered.block.coolingpond;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
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
public class CoolingPondBlockEntity extends BlockEntity {
    public static final int SLOT = 0;
    public static final int COOL_TICKS = 1200; // 60 seconds @ 20 TPS

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.HOT_SPENT_FUEL_ROD.get())
                    || stack.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get());
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();
    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int coolProgress = 0;

    public CoolingPondBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLING_POND.get(), pos, state);
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
        ItemStack stack = itemHandler.getStackInSlot(SLOT);
        if (stack.is(ModItems.HOT_SPENT_FUEL_ROD.get())) {
            coolProgress++;
            setChanged();
            if (coolProgress >= COOL_TICKS) {
                itemHandler.setStackInSlot(SLOT,
                        new ItemStack(ModItems.DEPLETED_URANIUM_FUEL_ROD.get(), stack.getCount()));
                coolProgress = 0;
            }
        } else {
            if (coolProgress != 0) {
                coolProgress = 0;
                setChanged();
            }
            if (stack.is(ModItems.DEPLETED_URANIUM_FUEL_ROD.get())) {
                autoPushOut(level, pos);
            }
        }
    }

    private void autoPushOut(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            ItemStack source = itemHandler.getStackInSlot(SLOT);
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

    // External wrapper — accepts hot rods only, emits depleted rods only.
    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.is(ModItems.HOT_SPENT_FUEL_ROD.get())) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
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
