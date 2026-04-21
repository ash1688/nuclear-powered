package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.electricfurnace.ElectricFurnaceBlockEntity;
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

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_AUTO_INPUT = 0;
    public static final int BUTTON_TOGGLE_AUTO_OUTPUT = 1;

    public final ElectricFurnaceBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public ElectricFurnaceMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(6));
    }

    public ElectricFurnaceMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.ELECTRIC_FURNACE.get(), id);
        this.blockEntity = (ElectricFurnaceBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                ElectricFurnaceBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                ElectricFurnaceBlockEntity.SLOT_OUTPUT, 116, 35));
        // Upgrade bay — accepts only the Furnace Speed Card.
        addSlot(new SlotItemHandler(blockEntity.getUpgradeHandlerForMenu(), 0, 134, 35));

        addDataSlots(data);
    }

    private static ElectricFurnaceBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof ElectricFurnaceBlockEntity furnace)) {
            throw new IllegalStateException("ElectricFurnaceMenu opened without an ElectricFurnaceBlockEntity at " + pos);
        }
        return furnace;
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
            case BUTTON_TOGGLE_AUTO_INPUT -> {
                blockEntity.toggleAutoInput();
                return true;
            }
            case BUTTON_TOGGLE_AUTO_OUTPUT -> {
                blockEntity.toggleAutoOutput();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        int machineInput = PLAYER_INV_SLOT_COUNT;
        int machineUpgrade = PLAYER_INV_SLOT_COUNT + 2;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            // Try upgrade bay first (Furnace Speed Card), then input slot.
            if (!moveItemStackTo(source, machineUpgrade, machineUpgrade + 1, false)
                    && !moveItemStackTo(source, machineInput, machineInput + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex <= machineUpgrade) {
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
                player, ModBlocks.ELECTRIC_FURNACE.get());
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
