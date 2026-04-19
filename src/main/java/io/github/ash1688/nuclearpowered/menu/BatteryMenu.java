package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.battery.BatteryBlockEntity;
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

public class BatteryMenu extends AbstractContainerMenu {
    public final BatteryBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public BatteryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(2));
    }

    public BatteryMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.BATTERY.get(), id);
        this.blockEntity = (BatteryBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addDataSlots(data);
    }

    private static BatteryBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof BatteryBlockEntity battery)) {
            throw new IllegalStateException("BatteryMenu opened without a BatteryBlockEntity at " + pos);
        }
        return battery;
    }

    public int getStoredFE() { return data.get(0); }

    public int getMaxFE() { return data.get(1); }

    public int getScaledFE(int barHeight) {
        int max = data.get(1);
        return (max == 0) ? 0 : data.get(0) * barHeight / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int hotbarStart = 27;
        int hotbarEnd = 36;
        if (slotIndex < hotbarStart) {
            if (!moveItemStackTo(source, hotbarStart, hotbarEnd, false)) return ItemStack.EMPTY;
        } else if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            if (!moveItemStackTo(source, 0, hotbarStart, false)) return ItemStack.EMPTY;
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
                player, ModBlocks.BATTERY.get());
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
