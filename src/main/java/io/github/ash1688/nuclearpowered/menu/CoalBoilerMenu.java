package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.boiler.CoalBoilerBlockEntity;
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

public class CoalBoilerMenu extends AbstractContainerMenu {
    public final CoalBoilerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    public CoalBoilerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(6));
    }

    public CoalBoilerMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.COAL_BOILER.get(), id);
        this.blockEntity = (CoalBoilerBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                CoalBoilerBlockEntity.SLOT_FUEL, 80, 26));
        addSlot(new SlotItemHandler(blockEntity.getItemHandlerForMenu(),
                CoalBoilerBlockEntity.SLOT_BUCKET, 80, 50));

        addDataSlots(data);
    }

    private static CoalBoilerBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof CoalBoilerBlockEntity boiler)) {
            throw new IllegalStateException("CoalBoilerMenu opened without a CoalBoilerBlockEntity at " + pos);
        }
        return boiler;
    }

    public int getWaterAmount() { return data.get(0); }
    public int getWaterCapacity() { return data.get(1); }
    public int getSteamAmount() { return data.get(2); }
    public int getSteamCapacity() { return data.get(3); }
    public int getBurnTime() { return data.get(4); }
    public int getMaxBurnTime() { return data.get(5); }

    public boolean isBurning() { return getBurnTime() > 0; }

    public int getScaledWater(int barHeight) {
        return getWaterCapacity() == 0 ? 0 : getWaterAmount() * barHeight / getWaterCapacity();
    }

    public int getScaledSteam(int barHeight) {
        return getSteamCapacity() == 0 ? 0 : getSteamAmount() * barHeight / getSteamCapacity();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        int machineFirst = PLAYER_INV_SLOT_COUNT;
        int machineLastExclusive = PLAYER_INV_SLOT_COUNT + 2;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            if (!moveItemStackTo(source, machineFirst, machineLastExclusive, false)) return ItemStack.EMPTY;
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
                player, ModBlocks.COAL_BOILER.get());
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
