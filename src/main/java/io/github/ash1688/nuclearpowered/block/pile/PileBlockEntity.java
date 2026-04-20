package io.github.ash1688.nuclearpowered.block.pile;

import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.menu.PileMenu;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class PileBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_DEPLETED = 1;

    // Burn duration per fuel rod at 1x speed (~200s / 3.3 min).
    public static final int BURN_TICKS_PER_ROD = 4000;

    // Heat model — applied once per second.
    //
    //  fuel (burning)     +30 /s
    //  casings below 3K   +5  /s each
    //  casings 3K-4K      -1  /s each
    //  casings 4K-5K      -2  /s each (cumulative)
    //  casings 5K-6K      -3  /s each
    //  casings 6K-7K      -4  /s each
    //  casings 7K+        -5  /s each
    //  thermo idle        -1  /s each      (attached, not draining FE)
    //  thermo active      -2  /s each      (someone pulled FE this second)
    //
    // Equilibrium naturally sits around 3000 with any real thermo load. No
    // slowdown curve — the casing-band mechanic does the self-limiting.
    private static final int HEAT_UPDATE_INTERVAL_TICKS = 20;
    private static final int FUEL_HEAT_PER_SEC = 30;
    private static final int CASING_HEAT_BELOW_3K = 5;
    private static final int HEAT_BAND_START = 3000;
    private static final int HEAT_BAND_WIDTH = 1000;
    private static final int MAX_HEAT = 10_000;

    // Multiblock structure scan parameters.
    private static final int STRUCTURE_SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_STRUCTURE_BLOCKS = 128;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_FUEL) return stack.is(ModItems.URANIUM_FUEL_ROD.get());
            return super.isItemValid(slot, stack);
        }
    };

    private final IItemHandler externalHandler = new SidedItemHandler();

    private LazyOptional<IItemHandler> lazyExternalHandler = LazyOptional.empty();

    private int heat = 0;
    private int burnTime = 0;           // burn ticks remaining on the current rod
    private int heatTickCounter = 0;    // 0..HEAT_UPDATE_INTERVAL_TICKS, fires the once-per-second heat update
    private int lastHeatDelta = 0;      // last computed per-second delta, shown in the GUI tooltip
    private boolean autoInput = true;
    private boolean autoOutput = true;

    // Multiblock scan cache. Recomputed on an interval to avoid hammering the world
    // on every tick. Not persisted — it's derived data.
    private int cachedCasingCount = 0;
    private int scanCooldown = 0;

    // Strict 3x3x3 shell validity. Updated every tick (cheap — 26 block reads).
    private boolean structureValid = false;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> getMaxHeat();
                case 2 -> burnTime;
                case 3 -> BURN_TICKS_PER_ROD;
                case 4 -> autoInput ? 1 : 0;
                case 5 -> autoOutput ? 1 : 0;
                case 6 -> lastHeatDelta;
                case 7 -> cachedCasingCount;
                case 8 -> structureValid ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> heat = value;
                case 2 -> burnTime = value;
                case 4 -> autoInput = value != 0;
                case 5 -> autoOutput = value != 0;
                // slowdown, max heat, casing count are derived — no setter.
            }
        }

        @Override
        public int getCount() { return 9; }
    };

    public PileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAPHITE_PILE.get(), pos, state);
    }

    public IItemHandler getItemHandlerForMenu() { return itemHandler; }

    public int getHeat() { return heat; }

    // Add heat from an external source (e.g. a heater block priming the pile
    // before the fuel rod gets it hot). Clamps to the pile's max heat so a
    // heater can't push the core past its MAX_HEAT safety ceiling.
    public void addHeat(int amount) {
        if (amount <= 0) return;
        int cap = getMaxHeat();
        if (heat >= cap) return;
        heat = Math.min(cap, heat + amount);
        setChanged();
    }

    // Remove heat from the pile (thermocouples call this when drawing energy, so
    // pulling power out of a reactor actually cools it). Returns how much was
    // removed, clamped to what's actually in the pile.
    public int drainHeat(int amount) {
        if (amount <= 0 || heat <= 0) return 0;
        int removed = Math.min(amount, heat);
        heat -= removed;
        setChanged();
        return removed;
    }

    public int getMaxHeat() { return MAX_HEAT; }

    public int getSafeHeat() { return HEAT_BAND_START; }

    public int getCasingCount() { return cachedCasingCount; }

    public boolean isStructureValid() { return structureValid; }

    public boolean isBurning() { return burnTime > 0; }

    public boolean isAutoInput() { return autoInput; }

    public boolean isAutoOutput() { return autoOutput; }

    public void toggleAutoInput() { autoInput = !autoInput; setChanged(); }

    public void toggleAutoOutput() { autoOutput = !autoOutput; setChanged(); }

    // Per-second heat coefficient each casing contributes at the current heat
    // level. Below 3K each casing heats (+5). At 3K and above each casing
    // cools, stacking an extra -1 for every 1K band crossed.
    private int casingCoefficient(int currentHeat) {
        if (currentHeat < HEAT_BAND_START) return CASING_HEAT_BELOW_3K;
        int bandsAbove = 1 + (currentHeat - HEAT_BAND_START) / HEAT_BAND_WIDTH;
        return -Math.min(5, bandsAbove);
    }

    public int getLastHeatDelta() { return lastHeatDelta; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nuclearpowered.graphite_pile");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PileMenu(id, inv, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyExternalHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyExternalHandler = LazyOptional.of(() -> externalHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyExternalHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("heat", heat);
        tag.putInt("burnTime", burnTime);
        tag.putInt("heatTickCounter", heatTickCounter);
        tag.putBoolean("autoInput", autoInput);
        tag.putBoolean("autoOutput", autoOutput);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        heat = tag.getInt("heat");
        burnTime = tag.getInt("burnTime");
        heatTickCounter = tag.getInt("heatTickCounter");
        autoInput = !tag.contains("autoInput") || tag.getBoolean("autoInput");
        autoOutput = !tag.contains("autoOutput") || tag.getBoolean("autoOutput");
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

        // Refresh the connected-casing count on a cadence so we don't re-scan every tick.
        if (scanCooldown-- <= 0) {
            rescanStructure(level);
            scanCooldown = STRUCTURE_SCAN_INTERVAL_TICKS;
        }

        // Cheap strict check every tick: all 26 neighbours must be graphite_casing.
        structureValid = checkStructureValid(level);

        // If the structure was broken mid-burn, kill the burn immediately. Residual
        // heat still cools naturally via casing coefficients.
        if (!structureValid && burnTime > 0) {
            burnTime = 0;
            changed = true;
        }

        // Fuel burns at a constant rate — one burn-tick per real tick. A rod
        // lasts BURN_TICKS_PER_ROD real ticks (~200 s at 20 TPS).
        if (burnTime > 0) {
            burnTime--;
            if (burnTime == 0) produceDepletedRod();
            changed = true;
        } else if (canStartBurn()) {
            itemHandler.getStackInSlot(SLOT_FUEL).shrink(1);
            burnTime = BURN_TICKS_PER_ROD;
            changed = true;
        }

        // Heat bookkeeping once per second — fuel contributes +30, each casing
        // contributes +5 below 3K or a cumulative penalty above it. Thermo
        // cooling is applied separately by each thermocouple on the same cadence.
        heatTickCounter++;
        if (heatTickCounter >= HEAT_UPDATE_INTERVAL_TICKS) {
            heatTickCounter = 0;
            int delta = (burnTime > 0 ? FUEL_HEAT_PER_SEC : 0)
                    + cachedCasingCount * casingCoefficient(heat);
            int oldHeat = heat;
            heat = Math.max(0, Math.min(MAX_HEAT, heat + delta));
            lastHeatDelta = heat - oldHeat; // reflects any clamping
            if (heat != oldHeat) changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
        }

        if (autoOutput && !itemHandler.getStackInSlot(SLOT_DEPLETED).isEmpty()) {
            autoPushSlot(level, pos, SLOT_DEPLETED);
        }
    }

    private void autoPushSlot(Level level, BlockPos pos, int slot) {
        for (Direction dir : Direction.values()) {
            ItemStack source = itemHandler.getStackInSlot(slot);
            if (source.isEmpty()) return;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            IItemHandler sink = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (sink == null) continue;
            ItemStack attempt = source.copy();
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(sink, attempt, false);
            int moved = attempt.getCount() - remaining.getCount();
            if (moved > 0) {
                source.shrink(moved);
                setChanged();
            }
        }
    }

    private boolean canStartBurn() {
        if (!structureValid) return false;
        if (itemHandler.getStackInSlot(SLOT_FUEL).isEmpty()) return false;
        if (!itemHandler.getStackInSlot(SLOT_FUEL).is(ModItems.URANIUM_FUEL_ROD.get())) return false;
        ItemStack depleted = itemHandler.getStackInSlot(SLOT_DEPLETED);
        if (depleted.isEmpty()) return true;
        if (!depleted.is(ModItems.HOT_SPENT_FUEL_ROD.get())) return false;
        return depleted.getCount() + 1 <= depleted.getMaxStackSize();
    }

    // Strict 3x3x3 shell check. The pile sits at one of the 4 horizontal face-centre
    // positions (N / E / S / W) of the cube so one side face is open for player
    // interaction. Top and bottom face-centres are intentionally NOT accepted —
    // piles need to be approached from a walkable side, not climbed onto. For each
    // candidate outward direction D, the structure centre is the neighbour OPPOSITE
    // that direction; every non-pile position in the 3x3x3 around that centre must
    // be a graphite_casing. Any matching orientation counts.
    private static final Direction[] VALID_OPEN_FACES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private boolean checkStructureValid(Level level) {
        if (level == null) return false;
        for (Direction d : VALID_OPEN_FACES) {
            BlockPos center = worldPosition.relative(d.getOpposite());
            if (isShellAround(level, center)) return true;
        }
        return false;
    }

    private boolean isShellAround(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (p.equals(worldPosition)) continue; // skip the pile itself
                    if (!level.getBlockState(p).is(ModBlocks.GRAPHITE_CASING.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void produceDepletedRod() {
        // Spent fuel emerges dangerously hot — feed it to a Cooling Pond before it
        // can be reprocessed (or shear it from a hotbar slot and accept the burns).
        ItemStack depleted = itemHandler.getStackInSlot(SLOT_DEPLETED);
        if (depleted.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_DEPLETED, new ItemStack(ModItems.HOT_SPENT_FUEL_ROD.get()));
        } else {
            depleted.grow(1);
        }
    }

    // Bounded flood-fill from this pile's position out through connected graphite_casing
    // blocks. Stops after MAX_STRUCTURE_BLOCKS visits to keep the cost cheap. The resulting
    // count drives the dynamic MAX_HEAT / SAFE_HEAT values.
    private void rescanStructure(Level level) {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            queue.offer(worldPosition.relative(dir));
        }
        int count = 0;
        while (!queue.isEmpty() && visited.size() < MAX_STRUCTURE_BLOCKS) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;
            BlockState bs = level.getBlockState(pos);
            if (bs.is(ModBlocks.GRAPHITE_CASING.get())) {
                count++;
                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.relative(dir);
                    if (!visited.contains(next)) queue.offer(next);
                }
            }
        }
        cachedCasingCount = count;
    }

    private final class SidedItemHandler implements IItemHandler {
        @Override public int getSlots() { return itemHandler.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!autoInput || slot != SLOT_FUEL) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!autoOutput || slot != SLOT_DEPLETED) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }
}
