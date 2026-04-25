package io.github.ash1688.nuclearpowered.block.pile.output;

import io.github.ash1688.nuclearpowered.block.pile.PileBlockEntity;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import io.github.ash1688.nuclearpowered.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

// Acts as a transparent conduit between the pile's depleted-rod slot and any
// adjacent item-handler block (Cooling Pond, chest, hopper). The port is part
// of the multiblock's shell — the pile won't have a direct ITEM_HANDLER side
// to push to since casings sit between the pile and the world, so the port
// pulls instead. Direction is auto-detected: every tick we scan our 6
// neighbours and push to the first non-pile/non-casing/non-port handler we
// find.
public class FuelRodOutputPortBlockEntity extends BlockEntity {
    // Re-find the connected pile on this cadence in case the structure was
    // broken or rebuilt. Cheap — single flood-fill bounded by SCAN_DISTANCE.
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_DISTANCE = 64;

    @Nullable private BlockPos cachedPilePos;
    private int scanCooldown = 0;

    public FuelRodOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_ROD_OUTPUT_PORT.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Periodic re-scan; also drop the cache if the cached pile has been mined.
        if (cachedPilePos != null) {
            BlockState cached = level.getBlockState(cachedPilePos);
            if (!cached.is(ModBlocks.GRAPHITE_PILE.get())) cachedPilePos = null;
        }
        if (scanCooldown-- <= 0 || cachedPilePos == null) {
            findConnectedPile(level);
            scanCooldown = SCAN_INTERVAL_TICKS;
        }
        if (cachedPilePos == null) return;

        BlockEntity pileBE = level.getBlockEntity(cachedPilePos);
        if (!(pileBE instanceof PileBlockEntity pile)) return;

        IItemHandler pileItems = pile.getItemHandlerForMenu();
        ItemStack depleted = pileItems.getStackInSlot(PileBlockEntity.SLOT_DEPLETED);
        if (depleted.isEmpty()) return;

        // Auto-detect output direction — push to the first neighbour that
        // exposes an item handler, skipping anything that's part of our own
        // multiblock (casings/ports/pile).
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
            ItemStack attempt = depleted.copy();
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(sink, attempt, false);
            int moved = attempt.getCount() - remaining.getCount();
            if (moved > 0) {
                pileItems.extractItem(PileBlockEntity.SLOT_DEPLETED, moved, false);
                return; // one push per tick is plenty — keeps the cadence calm
            }
        }
    }

    // Bounded flood-fill out through casings and other ports until a pile is
    // hit. Same pattern as the thermocouple's pile finder so behaviour stays
    // consistent for the player.
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
