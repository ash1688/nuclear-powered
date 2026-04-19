package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.engine.SteamEngineBlockEntity;
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

public class SteamEngineMenu extends AbstractContainerMenu {
    public final SteamEngineBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public SteamEngineMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(5));
    }

    public SteamEngineMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.STEAM_ENGINE.get(), id);
        this.blockEntity = (SteamEngineBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addDataSlots(data);
    }

    private static SteamEngineBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof SteamEngineBlockEntity engine)) {
            throw new IllegalStateException("SteamEngineMenu opened without a SteamEngineBlockEntity at " + pos);
        }
        return engine;
    }

    public int getSteamAmount() { return data.get(0); }
    public int getSteamCapacity() { return data.get(1); }
    public int getStoredFE() { return data.get(2); }
    public int getMaxFE() { return data.get(3); }
    public int getLastFEGenerated() { return data.get(4); }

    public int getScaledSteam(int barHeight) {
        return getSteamCapacity() == 0 ? 0 : getSteamAmount() * barHeight / getSteamCapacity();
    }

    public int getScaledFE(int barHeight) {
        return getMaxFE() == 0 ? 0 : getStoredFE() * barHeight / getMaxFE();
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
                player, ModBlocks.STEAM_ENGINE.get());
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
