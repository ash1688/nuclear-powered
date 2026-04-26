package io.github.ash1688.nuclearpowered.block.boiler;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import com.mojang.logging.LogUtils;
import io.github.ash1688.nuclearpowered.client.ui.NPMachineUI;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class CoalBoilerBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /** TEMPORARY debug interval (ticks). 0 = off. */
    private static final int DEBUG_LOG_INTERVAL = 40;
    private int debugTick = 0;

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_BUCKET = 1;

    // Tank sizes — 4 buckets each feels right for a small boiler.
    public static final int TANK_CAPACITY_MB = 4000;
    private static final int BUCKET_VOLUME_MB = 1000;

    // Boiler conversion rate while burning: consumes WATER_PER_TICK mB of water,
    // produces STEAM_PER_TICK mB of steam each tick. 1:2 ratio gives players a
    // slight "gain" from water (one bucket of water → two buckets of steam).
    private static final int WATER_PER_TICK = 1;
    private static final int STEAM_PER_TICK = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_FUEL) return ForgeHooks.getBurnTime(stack, null) > 0;
            if (slot == SLOT_BUCKET) return stack.is(Items.WATER_BUCKET);
            return super.isItemValid(slot, stack);
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY_MB,
            stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank steamTank = new io.github.ash1688.nuclearpowered.compat.gtceu.SteamTank(TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() { setChanged(); }

        // External fills are blocked by the combinedFluidHandler capability (which only
        // routes water into waterTank), so this tank can accept internal fill() calls
        // directly without an override.
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    // Exposed fluid capability: writes go to waterTank, reads/extracts come from steamTank.
    private final IFluidHandler combinedFluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : steamTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getCapacity() : steamTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? waterTank.isFluidValid(stack) : steamTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() == Fluids.WATER) return waterTank.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (io.github.ash1688.nuclearpowered.compat.gtceu.SteamCompat.isSteam(resource.getFluid())) {
                return steamTankDrain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steamTankDrain(maxDrain, action);
        }

        private FluidStack steamTankDrain(int maxDrain, FluidAction action) {
            FluidStack current = steamTank.getFluid();
            if (current.isEmpty()) return FluidStack.EMPTY;
            int drained = Math.min(maxDrain, current.getAmount());
            FluidStack result = new FluidStack(current.getFluid(), drained);
            if (action.execute()) {
                current.shrink(drained);
                if (current.isEmpty()) steamTank.setFluid(FluidStack.EMPTY);
                setChanged();
            }
            return result;
        }
    };

    private int burnTime = 0;         // ticks remaining on the current fuel item
    private int maxBurnTime = 0;      // total ticks the current fuel item provides

    public CoalBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_BOILER.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    @Override
    public BlockEntity self() { return this; }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(NPMachineUI.UI_W, NPMachineUI.UI_H, this, player);
        IItemTransfer machineItems = ItemTransferHelperImpl.toItemTransfer(itemHandler);

        NPMachineUI.addBackground(ui.mainGroup);
        NPMachineUI.addTitle(ui.mainGroup, "block.nuclearpowered.coal_boiler");

        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_FUEL, 56, 53, true, true));
        ui.mainGroup.addWidget(NPMachineUI.slot(machineItems, SLOT_BUCKET, 56, 17, true, true));

        // Burn-time progress bar (vertical-ish, render as horizontal bar above fuel slot).
        ui.mainGroup.addWidget(NPMachineUI.progressArrow(56, 41, 18,
                () -> burnTime, () -> maxBurnTime == 0 ? 1 : maxBurnTime));

        ui.mainGroup.addWidget(NPMachineUI.tankBar(96, 17, waterTank));
        ui.mainGroup.addWidget(NPMachineUI.tankBar(132, 17, steamTank));

        // Status line: green "Burning" while a coal item is on the burn timer,
        // grey "Idle" otherwise. Re-evaluates every frame from the live BE.
        ui.mainGroup.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 75,
                () -> burnTime > 0 ? "§aBurning" : "§7Idle")
                .setDropShadow(true));

        NPMachineUI.addPlayerInventory(ui.mainGroup, player);
        return ui;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> externalHandler);
        lazyFluidHandler = LazyOptional.of(() -> combinedFluidHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(waterTag);
        tag.put("water", waterTag);
        CompoundTag steamTag = new CompoundTag();
        steamTank.writeToNBT(steamTag);
        tag.put("steam", steamTag);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        waterTank.readFromNBT(tag.getCompound("water"));
        steamTank.readFromNBT(tag.getCompound("steam"));
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
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
        boolean changed = false;
        boolean log = DEBUG_LOG_INTERVAL > 0 && (debugTick++ % DEBUG_LOG_INTERVAL == 0);
        if (log) LOGGER.info("[NP-Boiler] tick — burnTime={}/{} water={} steam={} canBoil(pre)? pos={}",
                burnTime, maxBurnTime, waterTank.getFluidAmount(), steamTank.getFluidAmount(), pos);

        // Bucket slot auto-fills the water tank, 1000 mB at a time.
        ItemStack bucket = itemHandler.getStackInSlot(SLOT_BUCKET);
        if (bucket.is(Items.WATER_BUCKET)
                && waterTank.getFluidAmount() + BUCKET_VOLUME_MB <= waterTank.getCapacity()) {
            waterTank.fill(new FluidStack(Fluids.WATER, BUCKET_VOLUME_MB), IFluidHandler.FluidAction.EXECUTE);
            itemHandler.setStackInSlot(SLOT_BUCKET, new ItemStack(Items.BUCKET));
            changed = true;
        }

        // Consume fuel if we can make steam.
        boolean canBoil = waterTank.getFluidAmount() >= WATER_PER_TICK
                && steamTank.getFluidAmount() + STEAM_PER_TICK <= steamTank.getCapacity();

        if (burnTime <= 0 && canBoil) {
            ItemStack fuel = itemHandler.getStackInSlot(SLOT_FUEL);
            int burnValue = ForgeHooks.getBurnTime(fuel, null);
            if (burnValue > 0) {
                burnTime = burnValue;
                maxBurnTime = burnValue;
                fuel.shrink(1);
                changed = true;
            }
        }

        if (burnTime > 0 && canBoil) {
            burnTime--;
            waterTank.drain(WATER_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            // Emit GT's steam fluid when GT is loaded so the output can drive
            // GT's own steam machines directly (Steam Macerator / Bender /
            // Compressor etc.); otherwise stay on NP's own steam fluid.
            steamTank.fill(new FluidStack(
                    io.github.ash1688.nuclearpowered.compat.gtceu.SteamCompat.activeEmitFluid(),
                    STEAM_PER_TICK), IFluidHandler.FluidAction.EXECUTE);
            changed = true;
        }

        // Push steam to adjacent fluid handlers each tick.
        pushSteam(level, pos);

        if (changed) setChanged(level, pos, state);
    }

    private void pushSteam(Level level, BlockPos pos) {
        boolean log = DEBUG_LOG_INTERVAL > 0 && (debugTick % DEBUG_LOG_INTERVAL == 0);
        if (log) LOGGER.info("[NP-Boiler] pushSteam — tank has {} mB", steamTank.getFluidAmount());
        if (steamTank.getFluidAmount() <= 0) return;
        for (Direction dir : Direction.values()) {
            if (steamTank.getFluidAmount() <= 0) break;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) {
                if (log) LOGGER.info("[NP-Boiler]   {} -> no BE", dir);
                continue;
            }
            if (log) LOGGER.info("[NP-Boiler]   {} -> {}", dir, neighbour.getClass().getName());
            neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(sink -> {
                // Offer whatever fluid the tank currently holds — respects both
                // steam variants during a cross-version save transition.
                FluidStack current = steamTank.getFluid();
                if (current.isEmpty()) return;
                FluidStack offer = new FluidStack(current.getFluid(),
                        Math.min(current.getAmount(), 200));
                int accepted = sink.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (log) LOGGER.info("[NP-Boiler]     fluid={} offered={} accepted={}",
                        current.getFluid(), offer.getAmount(), accepted);
                if (accepted > 0) {
                    steamTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            });
        }
    }

    public FluidTank getWaterTank() { return waterTank; }
    public FluidTank getSteamTank() { return steamTank; }

    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Both slots accept external pushes (fuel + buckets automatable).
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Only empty buckets out of the bucket slot; no extracting fuel.
            if (slot == SLOT_BUCKET) return itemHandler.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
