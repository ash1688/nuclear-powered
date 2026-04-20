package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.coolingpond.CoolingPondBlockEntity;
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

public class CoolingPondMenu extends AbstractContainerMenu {
    public final CoolingPondBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public CoolingPondMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(2));
    }

    public CoolingPondMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.COOLING_POND.get(), id);
        this.blockEntity = (CoolingPondBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                CoolingPondBlockEntity.SLOT, 80, 35));

        addDataSlots(data);
    }

    private static CoolingPondBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof CoolingPondBlockEntity pond)) {
            throw new IllegalStateException("CoolingPondMenu opened without a CoolingPondBlockEntity at " + pos);
        }
        return pond;
    }

    public int getCoolProgress() { return data.get(0); }
    public int getCoolTicks() { return data.get(1); }

    public int getScaledProgress(int pixels) {
        int progress = data.get(0);
        int max = data.get(1);
        return (max == 0 || progress == 0) ? 0 : progress * pixels / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int pondSlot = PLAYER_INV_SLOT_COUNT;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            if (!moveItemStackTo(source, pondSlot, pondSlot + 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex == pondSlot) {
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
                player, ModBlocks.COOLING_POND.get());
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
