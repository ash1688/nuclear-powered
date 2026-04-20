package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
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

public class PileMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_AUTO_INPUT = 0;
    public static final int BUTTON_TOGGLE_AUTO_OUTPUT = 1;

    public final PileBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public PileMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(9));
    }

    public PileMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.GRAPHITE_PILE.get(), id);
        this.blockEntity = (PileBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                PileBlockEntity.SLOT_FUEL, 56, 35));
        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                PileBlockEntity.SLOT_DEPLETED, 116, 35));

        addDataSlots(data);
    }

    private static PileBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof PileBlockEntity pile)) {
            throw new IllegalStateException("PileMenu opened without a PileBlockEntity at " + pos);
        }
        return pile;
    }

    public int getHeat() { return data.get(0); }

    public int getMaxHeat() { return data.get(1); }

    public int getScaledHeat() {
        int heat = data.get(0);
        int max = data.get(1);
        int bar = 52;
        return (max == 0) ? 0 : heat * bar / max;
    }

    public boolean isBurning() { return data.get(2) > 0; }

    public int getScaledBurnProgress() {
        // Arrow fills as the current rod burns DOWN; when burnTime is full it's 0%, empty is 100%.
        int remaining = data.get(2);
        int total = data.get(3);
        int arrow = 24;
        if (total == 0) return 0;
        int consumed = total - remaining;
        return consumed * arrow / total;
    }

    public boolean isAutoInput() { return data.get(4) != 0; }

    public boolean isAutoOutput() { return data.get(5) != 0; }

    public int getHeatDelta() { return data.get(6); }

    public int getCasingCount() { return data.get(7); }

    public boolean isStructureValid() { return data.get(8) != 0; }

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

        int fuelSlot = PLAYER_INV_SLOT_COUNT;
        int depletedSlot = PLAYER_INV_SLOT_COUNT + 1;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            // Player inv → fuel slot only (depleted slot is output-only from player view)
            if (!moveItemStackTo(source, fuelSlot, fuelSlot + 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex <= depletedSlot) {
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
                player, ModBlocks.GRAPHITE_PILE.get());
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
