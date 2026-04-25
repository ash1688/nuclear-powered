package io.github.ash1688.nuclearpowered.block.converter;

import io.github.ash1688.nuclearpowered.compat.gtceu.GTCompat;
import io.github.ash1688.nuclearpowered.compat.gtceu.GTEnergyCompat;
import io.github.ash1688.nuclearpowered.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * FE &lt;-&gt; EU bidirectional converter.
 *
 * <p>Single FE buffer is the source of truth. The Forge {@link IEnergyStorage}
 * capability is exposed on every face (universal), and the GT {@link
 * com.gregtechceu.gtceu.api.capability.IEnergyContainer} capability is exposed
 * via {@link GTEnergyCompat} only when GT is loaded. Conversion is a fixed
 * 1 EU = {@link #FE_PER_EU} FE, matching GT CEu's canonical ratio.</p>
 *
 * <p>Tick logic: every tick, push any stored FE to neighbours exposing the
 * Forge {@code ENERGY} cap (standard cable discovery) and, if GT is loaded,
 * also push EU equivalents to neighbours exposing {@code
 * CAPABILITY_ENERGY_CONTAINER}. Both directions respect {@link
 * #TRANSFER_RATE_FE_PER_TICK}, which is the tier-gated throughput ceiling.</p>
 *
 * <p>R1.5 ships a fixed LV tier (128 FE/tick = 32 EU/tick throughput, 100 K FE
 * buffer = 25 K EU). Tier upgrades land in Phase F alongside the Tiered
 * Assembler.</p>
 */
public class EnergyConverterBlockEntity extends BlockEntity {
    /** Symmetric GT ratio — 1 EU = 4 FE, publicly visible for the adapter and tooltips. */
    public static final int FE_PER_EU = 4;

    /** Internal buffer capacity in FE. 100,000 FE = 25,000 EU at 4:1. */
    public static final int CAPACITY_FE = 100_000;

    /** LV-tier throughput cap on both faces. 128 FE/t = 32 EU/t = LV voltage × 1 A. */
    public static final int TRANSFER_RATE_FE_PER_TICK = 128;

    private int storedFE = 0;

    /** Forge-side energy adapter. Universal — always active. */
    private final IEnergyStorage externalEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int canAccept = Math.min(CAPACITY_FE - storedFE, Math.min(amount, TRANSFER_RATE_FE_PER_TICK));
            if (canAccept <= 0) return 0;
            if (!simulate) {
                storedFE += canAccept;
                setChanged();
            }
            return canAccept;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int canDrain = Math.min(storedFE, Math.min(maxExtract, TRANSFER_RATE_FE_PER_TICK));
            if (canDrain <= 0) return 0;
            if (!simulate) {
                storedFE -= canDrain;
                setChanged();
            }
            return canDrain;
        }

        @Override public int getEnergyStored() { return storedFE; }
        @Override public int getMaxEnergyStored() { return CAPACITY_FE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> lazyFE = LazyOptional.empty();

    /**
     * Per-face Forge ENERGY wrapper lazies. Each face gets its own
     * IEnergyStorage that delegates to {@link #externalEnergy} but stamps
     * {@link #lastReceiveTickFromFace} on accepted receives. Pass 2 reads
     * the timestamp to skip pushing FE back into the same face that just
     * fed us — handles the case where a buffered neighbour like a Battery
     * is on one side and a real consumer is on the other, breaking the
     * bidirectional oscillation that pure {@code canExtract}-skip would
     * solve at the cost of never charging adjacent batteries at all.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final LazyOptional<IEnergyStorage>[] perFaceLazies = new LazyOptional[6];

    /**
     * Game-time of the most recent accepted receive on each face. Compared
     * against {@code level.getGameTime()} in Pass 2 with a 1-tick stale
     * window, so an adjacent push-source (battery, generator) in continuous
     * operation is always skipped on the next push pass regardless of BE
     * tick order. Initial state of zeros means "never received"; after game
     * tick 1 the difference is always larger than the window so untouched
     * faces are eligible.
     */
    private final long[] lastReceiveTickFromFace = new long[6];

    /**
     * GT-side energy adapter lazy. Typed as a wildcard so the BE compiles
     * cleanly without GT on the classpath — the only code that materialises
     * this field is {@link GTEnergyCompat#createLazy(EnergyConverterBlockEntity)},
     * which itself only classloads when GT is present.
     */
    @Nullable private LazyOptional<?> lazyEU;

    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CONVERTER.get(), pos, state);
    }

    // --- Public accessors used by GTEnergyCompat's IEnergyContainer adapter ---

    public int getStoredFE() { return storedFE; }

    public int getCapacityFE() { return CAPACITY_FE; }

    public int getTransferRateFE() { return TRANSFER_RATE_FE_PER_TICK; }

    /** Non-simulating FE insertion — used by the GT adapter's EU-in path. */
    public int receiveFEInternal(int amount) {
        int canAccept = Math.min(CAPACITY_FE - storedFE, amount);
        if (canAccept <= 0) return 0;
        storedFE += canAccept;
        setChanged();
        return canAccept;
    }

    /** Non-simulating FE drain — used by the GT adapter's EU-out path. */
    public int extractFEInternal(int amount) {
        int canDrain = Math.min(storedFE, amount);
        if (canDrain <= 0) return 0;
        storedFE -= canDrain;
        setChanged();
        return canDrain;
    }

    // --- Capability routing ---

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            // Null-side queries (some Forge code asks without a face) get the
            // generic lazy that doesn't track per-face receives.
            return (side == null ? lazyFE : getOrCreateFaceLazy(side)).cast();
        }
        if (GTCompat.isLoaded() && GTEnergyCompat.isEnergyContainerCap(cap)) {
            return getOrCreateEULazy().cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<IEnergyStorage> getOrCreateFaceLazy(Direction side) {
        int idx = side.ordinal();
        LazyOptional<IEnergyStorage> existing = perFaceLazies[idx];
        if (existing != null) return existing;

        // Wrapper delegates everything to the shared externalEnergy, but
        // stamps the face's lastReceiveTick on accepted receives so Pass 2
        // can avoid pushing back to a neighbour that just fed us.
        IEnergyStorage wrapper = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                int accepted = externalEnergy.receiveEnergy(amount, simulate);
                if (accepted > 0 && !simulate && level != null) {
                    lastReceiveTickFromFace[idx] = level.getGameTime();
                }
                return accepted;
            }
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return externalEnergy.extractEnergy(maxExtract, simulate);
            }
            @Override public int getEnergyStored() { return externalEnergy.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return externalEnergy.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return externalEnergy.canExtract(); }
            @Override public boolean canReceive() { return externalEnergy.canReceive(); }
        };
        LazyOptional<IEnergyStorage> created = LazyOptional.of(() -> wrapper);
        perFaceLazies[idx] = created;
        return created;
    }

    private LazyOptional<?> getOrCreateEULazy() {
        if (lazyEU == null) lazyEU = GTEnergyCompat.createLazy(this);
        return lazyEU;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFE = LazyOptional.of(() -> externalEnergy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFE.invalidate();
        if (lazyEU != null) lazyEU.invalidate();
        for (int i = 0; i < perFaceLazies.length; i++) {
            if (perFaceLazies[i] != null) perFaceLazies[i].invalidate();
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("fe", storedFE);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedFE = tag.getInt("fe");
    }

    // --- Tick: push energy out to adjacent sinks ---

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Diagnostic probe runs first and visits every face regardless of
        // budget so a face that ends up empty-handed during the push pass
        // still gets discovered and logged. Decoupled from the push so we
        // can keep a single global FE budget without losing face visibility.
        diagnosticProbe(level, pos);

        if (storedFE <= 0) return;

        // Global budget — the converter is rated at TRANSFER_RATE_FE_PER_TICK
        // total. Per-face budgets caused net-negative flow because every
        // adjacent FE-receiver (NP Battery / Cable that fed us, plus a
        // downstream GT block) drained the buffer at up to 128 FE/tick each.
        int budget = Math.min(storedFE, TRANSFER_RATE_FE_PER_TICK);
        if (budget <= 0) return;

        // Pass 1 — GT-only sinks (no Forge ENERGY cap exposed). These are
        // typically GT machines that only speak EU; they're the ones the
        // converter is *for*. Pushing here first stops an adjacent NP
        // Battery / Cable from soaking up the whole budget before the
        // EU consumer gets a turn.
        if (GTCompat.isLoaded()) {
            for (Direction dir : Direction.values()) {
                if (budget <= 0) break;
                BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
                if (neighbour == null) continue;
                Direction facing = dir.getOpposite();
                IEnergyStorage feSink = neighbour.getCapability(ForgeCapabilities.ENERGY, facing).orElse(null);
                if (feSink != null && feSink.canReceive()) continue; // pass 2 will handle this
                int pushedFE = GTEnergyCompat.pushToNeighbour(this, neighbour, facing, budget);
                if (pushedFE > 0) {
                    storedFE -= pushedFE;
                    budget -= pushedFE;
                    setChanged();
                }
            }
        }

        // Pass 2 — Forge FE sinks. Per-face received-tick check skips a
        // neighbour that pushed FE into us within the last game tick,
        // preventing the FE from bouncing straight back via Forge
        // ENERGY's bidirectional design. Other batteries (not currently
        // pushing) are still eligible, so the converter can charge a
        // downstream buffer when an upstream source is on a different
        // face.
        long now = level.getGameTime();
        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            // Bidirectional Forge ENERGY would otherwise oscillate FE
            // back to whatever just fed us. The 1-tick window absorbs
            // BE iteration order: even if Converter ticks before its
            // source within the same game tick, the next tick's pass 2
            // will see the stale flag and skip the back-push.
            if (now - lastReceiveTickFromFace[dir.ordinal()] <= 1) continue;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            Direction facing = dir.getOpposite();
            IEnergyStorage feSink = neighbour.getCapability(ForgeCapabilities.ENERGY, facing).orElse(null);
            if (feSink == null || !feSink.canReceive()) continue;
            int pushed = feSink.receiveEnergy(budget, false);
            if (pushed > 0) {
                storedFE -= pushed;
                budget -= pushed;
                setChanged();
            }
        }
    }

    // One-shot probe — each direction is logged at most once per JVM
    // session with whatever capabilities the neighbour exposes. Tells us
    // exactly what's on each face regardless of how the push pass
    // distributes the budget.
    private void diagnosticProbe(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (TICK_LOGGED.putIfAbsent(dir, Boolean.TRUE) != null) continue;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) {
                TICK_LOG.info("[NP/Converter] dir={} no neighbour BE", dir);
                continue;
            }
            Direction facing = dir.getOpposite();
            IEnergyStorage feSink = neighbour.getCapability(ForgeCapabilities.ENERGY, facing).orElse(null);
            boolean hasGtEnergy = GTCompat.isLoaded()
                    && GTEnergyCompat.hasEnergyContainer(neighbour, facing);
            TICK_LOG.info("[NP/Converter] dir={} neighbourClass={} forgeENERGY={} (canReceive={}) gtENERGY={}",
                    dir, neighbour.getClass().getName(),
                    feSink != null,
                    feSink != null && feSink.canReceive(),
                    hasGtEnergy);
        }
    }

    // First-tick-per-face logging guard. Once each direction has logged we
    // stay quiet to avoid log spam.
    private static final java.util.concurrent.ConcurrentHashMap<Direction, Boolean> TICK_LOGGED =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final org.slf4j.Logger TICK_LOG =
            com.mojang.logging.LogUtils.getLogger();
}
