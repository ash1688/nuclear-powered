package io.github.ash1688.nuclearpowered.block.fabricator;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModRecipes;
import io.github.ash1688.nuclearpowered.menu.FuelFabricatorMenu;
import io.github.ash1688.nuclearpowered.recipe.FuelFabricatorRecipe;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class FuelFabricatorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_CLADDING = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INPUT_PER_TICK = 256;
    public static final int FE_PER_TICK = 40;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = FuelFabricatorRecipe.DEFAULT_PROCESSING_TIME;
    private boolean autoInput = true;
    private boolean autoOutput = true;

    private int storedFE = 0;

    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accept = Math.min(ENERGY_CAPACITY - storedFE, Math.min(amount, ENERGY_MAX_INPUT_PER_TICK));
            if (accept <= 0) return 0;
            if (!simulate) {
                storedFE += accept;
                setChanged();
            }
            return accept;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> autoInput ? 1 : 0;
                case 3 -> autoOutput ? 1 : 0;
                case 4 -> storedFE;
                case 5 -> ENERGY_CAPACITY;
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
                case 4 -> storedFE = value;
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    public FuelFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_FABRICATOR.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public boolean isAutoInput() { return autoInput; }
    public boolean isAutoOutput() { return autoOutput; }
    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }
    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.fuel_fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FuelFabricatorMenu(id, inv, this, data);
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
        tag.putInt("progress", progress);
        tag.putInt("fe", storedFE);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        storedFE = tag.getInt("fe");
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
        Optional<RecipeMatch> match = findMatchingRecipe(level);
        boolean fabricatingThisTick = false;
        if (match.isPresent() && canFit(match.get().recipe.getResult())) {
            maxProgress = match.get().recipe.getProcessingTime();
            if (storedFE >= FE_PER_TICK) {
                storedFE -= FE_PER_TICK;
                progress++;
                setChanged(level, pos, state);
                if (progress >= maxProgress) {
                    craft(match.get());
                    progress = 0;
                }
            }
            fabricatingThisTick = true;
        }
        if (!fabricatingThisTick && progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }

        if (autoOutput && !itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            autoPushSlot(level, pos, SLOT_OUTPUT);
        }
    }

    // Recipe match plus which physical slot is currently holding the fuel item —
    // lets players load ingredients into either slot and still have the machine
    // craft correctly. The swapped flag just redirects the shrink calls in craft().
    private record RecipeMatch(FuelFabricatorRecipe recipe, boolean swapped) {}

    private Optional<RecipeMatch> findMatchingRecipe(Level level) {
        ItemStack a = itemHandler.getStackInSlot(SLOT_FUEL);
        ItemStack b = itemHandler.getStackInSlot(SLOT_CLADDING);
        if (a.isEmpty() || b.isEmpty()) return Optional.empty();
        SimpleContainer probe = new SimpleContainer(2);
        probe.setItem(0, a);
        probe.setItem(1, b);
        Optional<FuelFabricatorRecipe> direct = level.getRecipeManager()
                .getRecipeFor(ModRecipes.FABRICATING_TYPE.get(), probe, level);
        if (direct.isPresent()) return Optional.of(new RecipeMatch(direct.get(), false));
        probe.setItem(0, b);
        probe.setItem(1, a);
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.FABRICATING_TYPE.get(), probe, level)
                .map(r -> new RecipeMatch(r, true));
    }

    private boolean canFit(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(RecipeMatch match) {
        FuelFabricatorRecipe recipe = match.recipe();
        ItemStack result = recipe.getResult();
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        int fuelSlot = match.swapped() ? SLOT_CLADDING : SLOT_FUEL;
        int claddingSlot = match.swapped() ? SLOT_FUEL : SLOT_CLADDING;
        itemHandler.getStackInSlot(fuelSlot).shrink(recipe.getFuelCount());
        itemHandler.getStackInSlot(claddingSlot).shrink(recipe.getCladdingCount());
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

    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput) return stack;
            if (slot != SLOT_FUEL && slot != SLOT_CLADDING) return stack;
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
