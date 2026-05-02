package io.github.ash1688.nuclearpowered.block.creative;

import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

// Infinite EU source for testing the dual-energy system in EU mode. No-op
// when GTCEU isn't loaded — block still places, just doesn't push (the EU
// capability doesn't exist without GT). Pushes at HV with Long.MAX_VALUE
// amperage so the neighbour's own input cap is the limit.
public class CreativeEUGeneratorBlockEntity extends BlockEntity {
    /** Phantom GT IEnergyContainer so cables (and other EU-aware blocks)
     *  detect us as an EU endpoint. Real flow goes via {@link #tick}'s
     *  GTEnergyCompat.creativePush; this lazy is the connection signal. */
    @SuppressWarnings("rawtypes")
    private LazyOptional lazyEUProducer = LazyOptional.empty();

    public CreativeEUGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_EU_GENERATOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (GTCompat.isLoaded()) {
            lazyEUProducer = GTEnergyCompat.creativeEUProducerCap();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEUProducer.invalidate();
    }

    /**
     * Explicit empty Forge ENERGY response so external mods (GTCEU, etc.)
     * can't auto-shim a Forge ENERGY cap onto this block. EU generators are
     * EU-only; FE cables must refuse to connect, and our cable's canConnect
     * uses the Forge ENERGY cap as the connection signal. The GT cap goes
     * through the phantom EU producer above.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return LazyOptional.empty();
        if (GTCompat.isLoaded() && GTEnergyCompat.isEnergyContainerCap(cap)) {
            return (LazyOptional<T>) lazyEUProducer;
        }
        return super.getCapability(cap, side);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (!GTCompat.isLoaded()) return;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            GTEnergyCompat.creativePush(neighbour, dir.getOpposite());
        }
    }
}
