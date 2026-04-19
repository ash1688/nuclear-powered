package io.github.ash1688.nuclearpowered.block.washer;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import io.github.ash1688.nuclearpowered.menu.WasherMenu;
import io.github.ash1688.nuclearpowered.recipe.WasherRecipe;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class WasherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BUCKET = 2;

    public static final int TANK_CAPACITY_MB = 4000;
    private static final int BUCKET_VOLUME_MB = 1000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_BUCKET) return stack.is(Items.WATER_BUCKET);
            return super.isItemValid(slot, stack);
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

    private int progress = 0;
    private int maxProgress = WasherRecipe.DEFAULT_PROCESSING_TIME;
    private boolean autoInput = true;
    private boolean autoOutput = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> waterTank.getFluidAmount();
                case 3 -> waterTank.getCapacity();
                case 4 -> autoInput ? 1 : 0;
                case 5 -> autoOutput ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 4 -> autoInput = value != 0;
                case 5 -> autoOutput = value != 0;
                // 2 and 3 driven by tank; no setters.
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    public WasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public FluidTank getWaterTank() { return waterTank; }

    public boolean isAutoInput() { return autoInput; }

    public boolean isAutoOutput() { return autoOutput; }

    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }

    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.washer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WasherMenu(id, inv, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyExternalHandler = LazyOptional.of(() -> externalHandler);
        lazyFluidHandler = LazyOptional.of(() -> waterTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        CompoundTag tankTag = new CompoundTag();
        waterTank.writeToNBT(tankTag);
        tag.put("water", tankTag);
        tag.putInt("progress", progress);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        waterTank.readFromNBT(tag.getCompound("water"));
        progress = tag.getInt("progress");
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
        ItemStack bucket = itemHandler.getStackInSlot(SLOT_BUCKET);
        if (bucket.is(Items.WATER_BUCKET)
                && waterTank.getFluidAmount() + BUCKET_VOLUME_MB <= waterTank.getCapacity()) {
            waterTank.fill(new FluidStack(Fluids.WATER, BUCKET_VOLUME_MB), IFluidHandler.FluidAction.EXECUTE);
            itemHandler.setStackInSlot(SLOT_BUCKET, new ItemStack(Items.BUCKET));
            setChanged(level, pos, state);
        }

        Optional<WasherRecipe> recipe = findMatchingRecipe(level);
        if (recipe.isPresent() && canFit(recipe.get().getResult()) && hasEnoughFluid(recipe.get().getFluid())) {
            maxProgress = recipe.get().getProcessingTime();
            progress++;
            setChanged(level, pos, state);
            if (progress >= maxProgress) {
                craft(recipe.get());
                progress = 0;
            }
        } else if (progress != 0) {
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
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
