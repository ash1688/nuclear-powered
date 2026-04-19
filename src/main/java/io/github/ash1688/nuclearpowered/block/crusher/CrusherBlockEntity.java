package io.github.ash1688.nuclearpowered.block.crusher;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.menu.CrusherMenu;
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

public class CrusherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int MAX_PROGRESS = 40;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = MAX_PROGRESS;

    // ContainerData bridges server-side progress/maxProgress to the client screen.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
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
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    // Server-tick entry. Called from CrusherBlock.getTicker.
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (hasInput() && canOutput()) {
            progress++;
            setChanged(level, pos, state);
            if (progress >= maxProgress) {
                craft();
                progress = 0;
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged(level, pos, state);
        }
    }

    private boolean hasInput() {
        return !itemHandler.getStackInSlot(SLOT_INPUT).isEmpty();
    }

    // True if the output slot can accept another copy of whatever's in input.
    private boolean canOutput() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, input)) return false;
        return output.getCount() + 1 <= output.getMaxStackSize();
    }

    // Hardcoded passthrough recipe: consume one from input, produce one of the same thing in output.
    // Real recipe logic (option D) will replace this with a data-driven lookup.
    private void craft() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(input.getItem(), 1));
        } else {
            output.grow(1);
        }
        input.shrink(1);
    }
}
