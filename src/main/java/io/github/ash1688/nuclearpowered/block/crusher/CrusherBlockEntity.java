package io.github.ash1688.nuclearpowered.block.crusher;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import io.github.ash1688.nuclearpowered.menu.CrusherMenu;
import io.github.ash1688.nuclearpowered.recipe.CrusherRecipe;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int MAX_PROGRESS = CrusherRecipe.DEFAULT_PROCESSING_TIME;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
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

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> autoInput ? 1 : 0;
                case 3 -> autoOutput ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> autoInput = value != 0;
                case 3 -> autoOutput = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() {
        return itemHandler;
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrusherMenu(id, inv, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyExternalHandler.cast();
        }
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
        tag.putInt("progress", progress);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        // Default to true for worlds created before the toggles existed.
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
        Optional<CrusherRecipe> recipe = findMatchingRecipe(level);
        if (recipe.isPresent() && canFit(recipe.get().getResult())) {
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
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
