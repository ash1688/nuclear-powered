package io.github.ash1688.nuclearpowered.block.pile.output;

import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
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

// Pile-shell port that buffers and forwards depleted fuel rods. Pulls from
// the connected pile's depleted slot into a single internal buffer; each
// tick, tries to push the buffer's contents into the first adjacent
// item-handler that isn't part of the multiblock (Cooling Pond, chest,
// hopper). The buffer persists in NBT and drops if the port is broken, so a
// rod parked in a port whose pond is offline isn't lost.
public class FuelRodOutputPortBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_DISTANCE = 64;

    // 1-slot internal buffer — depleted rod waiting to be pushed out.
    private final ItemStackHandler buffer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public int getSlotLimit(int slot) { return 64; }
    };

    private LazyOptional<IItemHandler> lazyBuffer = LazyOptional.empty();

    @Nullable private BlockPos cachedPilePos;
    private int scanCooldown = 0;

    public FuelRodOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_ROD_OUTPUT_PORT.get(), pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        // Expose the buffer to external pipes/hoppers so an explicit suction
        // pipe can pull from this port if you'd rather not auto-push to the
        // adjacent block. Same handler on every face — simple and predictable.
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyBuffer.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyBuffer = LazyOptional.of(() -> buffer);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyBuffer.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("buffer", buffer.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("buffer")) buffer.deserializeNBT(tag.getCompound("buffer"));
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(buffer.getSlots());
        for (int i = 0; i < buffer.getSlots(); i++) inv.setItem(i, buffer.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, inv);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Refresh the connected pile cache periodically; drop it if the cached
        // pile position no longer holds a pile block.
        if (cachedPilePos != null) {
            BlockState cached = level.getBlockState(cachedPilePos);
            if (!cached.is(ModBlocks.GRAPHITE_PILE.get())) cachedPilePos = null;
        }
        if (scanCooldown-- <= 0 || cachedPilePos == null) {
            findConnectedPile(level);
            scanCooldown = SCAN_INTERVAL_TICKS;
        }

        // Pull from pile → buffer if there's room.
        if (cachedPilePos != null && buffer.getStackInSlot(0).getCount() < buffer.getSlotLimit(0)) {
            BlockEntity pileBE = level.getBlockEntity(cachedPilePos);
            if (pileBE instanceof PileBlockEntity pile) {
                IItemHandler pileItems = pile.getItemHandlerForMenu();
                ItemStack depleted = pileItems.getStackInSlot(PileBlockEntity.SLOT_DEPLETED);
                if (!depleted.isEmpty()) {
                    ItemStack buffered = buffer.getStackInSlot(0);
                    boolean compatible = buffered.isEmpty()
                            || (ItemStack.isSameItemSameTags(buffered, depleted)
                                && buffered.getCount() < buffer.getSlotLimit(0));
                    if (compatible) {
                        ItemStack extracted = pileItems.extractItem(PileBlockEntity.SLOT_DEPLETED, 1, false);
                        if (!extracted.isEmpty()) {
                            ItemStack remainder = buffer.insertItem(0, extracted, false);
                            // Defensive: if for some reason the buffer didn't
                            // accept it (race with a manual extract), put it
                            // back so the rod isn't voided.
                            if (!remainder.isEmpty()) {
                                pileItems.insertItem(PileBlockEntity.SLOT_DEPLETED, remainder, false);
                            }
                        }
                    }
                }
            }
        }

        // Push from buffer → adjacent item handler. Skip the multiblock's own
        // blocks so we never push back into the structure.
        ItemStack outgoing = buffer.getStackInSlot(0);
        if (outgoing.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighbourPos = pos.relative(dir);
            BlockState ns = level.getBlockState(neighbourPos);
            if (ns.is(ModBlocks.GRAPHITE_CASING.get())
                    || ns.is(ModBlocks.GRAPHITE_PILE.get())
                    || ns.is(ModBlocks.FUEL_ROD_OUTPUT_PORT.get())) continue;
            BlockEntity neighbour = level.getBlockEntity(neighbourPos);
            if (neighbour == null) continue;
            IItemHandler sink = neighbour.getCapability(
                    ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (sink == null) continue;
            ItemStack attempt = outgoing.copy();
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(sink, attempt, false);
            int moved = attempt.getCount() - remaining.getCount();
            if (moved > 0) {
                buffer.extractItem(0, moved, false);
                return; // one push per tick — calm cadence
            }
        }
    }

    // Bounded flood-fill outward through casings and other ports. First pile
    // hit wins. Same pattern the thermocouple uses to find its connected pile.
    private void findConnectedPile(Level level) {
        cachedPilePos = null;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            queue.offer(worldPosition.relative(dir));
        }
        while (!queue.isEmpty() && visited.size() < SCAN_DISTANCE) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) continue;
            BlockState bs = level.getBlockState(p);
            if (bs.is(ModBlocks.GRAPHITE_PILE.get())) {
                cachedPilePos = p;
                return;
            }
            if (bs.is(ModBlocks.GRAPHITE_CASING.get())
                    || bs.is(ModBlocks.FUEL_ROD_OUTPUT_PORT.get())) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = p.relative(dir);
                    if (!visited.contains(next)) queue.offer(next);
                }
            }
        }
    }
}
