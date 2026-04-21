package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.shearer.ShearerBlockEntity;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class ShearerMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_AUTO_INPUT = 0;
    public static final int BUTTON_TOGGLE_AUTO_OUTPUT = 1;

    public final ShearerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public ShearerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(6));
    }

    public ShearerMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.SHEARER.get(), id);
        this.blockEntity = (ShearerBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                ShearerBlockEntity.SLOT_INPUT, 44, 35));
        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                ShearerBlockEntity.SLOT_OUTPUT_FUEL, 104, 26));
        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                ShearerBlockEntity.SLOT_OUTPUT_SCRAP, 104, 44));
        // Upgrade bay — accepts only the Shearer Speed Card.
        addSlot(new SlotItemHandler(blockEntity.getUpgradeHandlerForMenu(), 0, 134, 35));

        addDataSlots(data);
    }

    private static ShearerBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof ShearerBlockEntity shearer)) {
            throw new IllegalStateException("ShearerMenu opened without a ShearerBlockEntity at " + pos);
        }
        return shearer;
    }

    public boolean isCrafting() { return data.get(0) > 0; }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowWidth = 24;
        return (maxProgress == 0 || progress == 0) ? 0 : progress * arrowWidth / maxProgress;
    }

    public boolean isAutoInput() { return data.get(2) != 0; }
    public boolean isAutoOutput() { return data.get(3) != 0; }
    public int getStoredFE() { return data.get(4); }
    public int getMaxFE() { return data.get(5); }

    public int getScaledFE(int barHeight) {
        int max = data.get(5);
        return (max == 0) ? 0 : data.get(4) * barHeight / max;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide) return false;
        switch (id) {
            case BUTTON_TOGGLE_AUTO_INPUT -> { blockEntity.toggleAutoInput(); return true; }
            case BUTTON_TOGGLE_AUTO_OUTPUT -> { blockEntity.toggleAutoOutput(); return true; }
            default -> { return false; }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int machineFirst = PLAYER_INV_SLOT_COUNT;
        int machineUpgrade = PLAYER_INV_SLOT_COUNT + 3;
        int machineLastExclusive = PLAYER_INV_SLOT_COUNT + 4;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            // Try upgrade bay first (Shearer Speed Card), then input slot.
            if (!moveItemStackTo(source, machineUpgrade, machineUpgrade + 1, false)
                    && !moveItemStackTo(source, machineFirst, machineFirst + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < machineLastExclusive) {
            if (!moveItemStackTo(source, 0, PLAYER_INV_SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, source);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SHEARER.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }
}
