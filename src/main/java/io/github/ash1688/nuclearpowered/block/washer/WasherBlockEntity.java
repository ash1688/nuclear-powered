package io.github.ash1688.nuclearpowered.block.washer;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class WasherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BUCKET = 2;

    public static final int TANK_CAPACITY_MB = 4000;
    private static final int BUCKET_VOLUME_MB = 1000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Only accept water buckets in the bucket slot so players/hoppers can't jam it.
            if (slot == SLOT_BUCKET) {
                return stack.is(Items.WATER_BUCKET);
            }
            return super.isItemValid(slot, stack);
        }
    };

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY_MB,
            stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    public WasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() {
        return itemHandler;
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.washer");
    }

    // Menu creation lands in commit 2.
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyFluidHandler = LazyOptional.of(() -> waterTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        CompoundTag tankTag = new CompoundTag();
        waterTank.writeToNBT(tankTag);
        tag.put("water", tankTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        waterTank.readFromNBT(tag.getCompound("water"));
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
        // Water in the tank is intentionally discarded on break — matches how vanilla
        // cauldrons/furnace fuel behave, and avoids dropping stray water items.
    }

    // Server-tick entry wired via WasherBlock.getTicker.
    public void tick(Level level, BlockPos pos, BlockState state) {
        // Bucket slot → tank auto-drain. One bucket per tick, only if there's room.
        ItemStack bucket = itemHandler.getStackInSlot(SLOT_BUCKET);
        if (bucket.is(Items.WATER_BUCKET)
                && waterTank.getFluidAmount() + BUCKET_VOLUME_MB <= waterTank.getCapacity()) {
            waterTank.fill(new FluidStack(Fluids.WATER, BUCKET_VOLUME_MB), IFluidHandler.FluidAction.EXECUTE);
            itemHandler.setStackInSlot(SLOT_BUCKET, new ItemStack(Items.BUCKET));
            setChanged(level, pos, state);
        }

        // Washing logic lands in commit 2 along with the WasherRecipe lookup.
    }
}
