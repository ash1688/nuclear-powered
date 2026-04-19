package io.github.ash1688.nuclearpowered.menu;

import io.github.ash1688.nuclearpowered.block.crusher.CrusherBlockEntity;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class CrusherMenu extends AbstractContainerMenu {
    public final CrusherBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INV_SLOT_COUNT = 36;

    // Client-side constructor invoked by IForgeMenuType.create.
    public CrusherMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveBlockEntity(inv, buf.readBlockPos()), new SimpleContainerData(2));
    }

    // Server-side constructor invoked from CrusherBlockEntity.createMenu.
    public CrusherMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.CRUSHER.get(), id);
        this.blockEntity = (CrusherBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            addSlot(new SlotItemHandler(handler, CrusherBlockEntity.SLOT_INPUT, 56, 35));
            addSlot(new SlotItemHandler(handler, CrusherBlockEntity.SLOT_OUTPUT, 116, 35));
        });

        addDataSlots(data);
    }

    private static CrusherBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (!(be instanceof CrusherBlockEntity crusher)) {
            throw new IllegalStateException("CrusherMenu opened for a block that has no CrusherBlockEntity at " + pos);
        }
        return crusher;
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    // Returns the filled width in pixels for the progress arrow on-screen.
    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowWidth = 24;
        return (maxProgress == 0 || progress == 0) ? 0 : progress * arrowWidth / maxProgress;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        int crusherInput = PLAYER_INV_SLOT_COUNT;
        int crusherOutput = PLAYER_INV_SLOT_COUNT + 1;

        if (slotIndex < PLAYER_INV_SLOT_COUNT) {
            // From player inventory → crusher input
            if (!moveItemStackTo(source, crusherInput, crusherInput + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex <= crusherOutput) {
            // From crusher → player inventory
            if (!moveItemStackTo(source, 0, PLAYER_INV_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
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
                player, ModBlocks.CRUSHER.get());
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
