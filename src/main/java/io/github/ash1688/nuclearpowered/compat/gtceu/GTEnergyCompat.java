package io.github.ash1688.nuclearpowered.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import io.github.ash1688.nuclearpowered.block.cable.EnergyCableBlockEntity;
import io.github.ash1688.nuclearpowered.block.converter.EnergyConverterBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * EU-side adapter for {@link EnergyConverterBlockEntity}.
 *
 * <p>This class references GT types freely — the guard is that nothing inside
 * NP references {@code GTEnergyCompat} without first passing {@code
 * GTCompat.isLoaded()}. The JVM only classloads on first reference, so when
 * GT is absent, this file's bytecode is never linked and the missing GT
 * classes don't matter.</p>
 *
 * <p>The adapter translates between GT's voltage × amperage energy model and
 * the converter's flat FE buffer. Conversion ratio is fixed at
 * {@link EnergyConverterBlockEntity#FE_PER_EU} (= 4), matching GT CEu's
 * canonical 1 EU = 4 FE.</p>
 */
public final class GTEnergyCompat {
    private GTEnergyCompat() {}

    /** Capability-token match test — used by the BE's getCapability router. */
    public static boolean isEnergyContainerCap(Capability<?> cap) {
        return cap == GTCapability.CAPABILITY_ENERGY_CONTAINER;
    }

    /**
     * Does this neighbour expose GT's IEnergyContainer on the given face?
     * The converter uses this to detect GT machines/buffers that also
     * happen to expose Forge ENERGY as a compat shim — for those, we
     * route via the GT cap exclusively because the Forge shim on a GT
     * battery buffer with no batteries silently voids any FE pushed in.
     */
    public static boolean hasEUCapability(BlockEntity neighbour, Direction facing) {
        return neighbour.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, facing).isPresent();
    }

    /**
     * "External GT-aware" — the neighbour exposes GT's IEnergyContainer AND
     * isn't an NP block. NP-side energy producers use this to skip pushing
     * FE into batteryless GT buffers (which void it via the Forge ENERGY
     * shim) without accidentally also skipping each other: GT CEu auto-adds
     * an IEnergyContainer compat shim onto every block that exposes Forge
     * ENERGY, so NP blocks themselves look GT-aware when GT is loaded.
     *
     * <p>Whitelisting by package name is intentional — registry lookups in
     * 1.20.1 Forge can return null at runtime depending on init order, but
     * a BE's runtime class is always its real implementation class, and
     * every NP BE lives under the {@code io.github.ash1688.nuclearpowered}
     * package tree. Foreign GT-aware BEs (battery buffers, generators,
     * transformers) live elsewhere, so the prefix check cleanly separates
     * "ours" from "theirs".</p>
     */
    public static boolean isExternalGTSink(BlockEntity neighbour, Direction facing) {
        if (!hasEUCapability(neighbour, facing)) return false;
        return !neighbour.getClass().getName()
                .startsWith("io.github.ash1688.nuclearpowered.");
    }

    /**
     * Creative EU push — used by the Creative EU Generator. Throttled to
     * 8 EU/tick (8 V × 1 A, ULV tier) per face so the receiver's buffer
     * bar fills at a clearly visible rate during testing. NP machines in
     * EU mode will accept ULV; vanilla GT machines that reject below-LV
     * inputs may need a higher tier — bump the voltage constant to 32
     * (LV) if that comes up.
     */
    public static boolean creativePush(BlockEntity neighbour, Direction facing) {
        IEnergyContainer sink = neighbour.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, facing).orElse(null);
        if (sink == null || !sink.inputsEnergy(facing)) return false;
        long accepted = sink.acceptEnergyFromNetwork(facing, 8L, 1L);
        return accepted > 0;
    }

    /** Build the LazyOptional the BE hands out when GT asks for EU. */
    public static LazyOptional<IEnergyContainer> createLazy(EnergyConverterBlockEntity be) {
        return LazyOptional.of(() -> new Adapter(be));
    }

    /**
     * Wrap a cable BE as an {@link IEnergyContainer} so EU producers can
     * push into the cable network. The adapter forwards
     * {@code acceptEnergyFromNetwork} into the cable's {@code distributeEU}
     * BFS, which walks the connected cables and dispatches the energy to
     * EU-capable neighbours.
     */
    public static LazyOptional<IEnergyContainer> wrapCableAsEU(EnergyCableBlockEntity cable) {
        return LazyOptional.of(() -> new CableAdapter(cable));
    }

    /**
     * Push EU into a single neighbour. Returns the amperage accepted
     * (0 if neighbour doesn't take EU on that face). Used by
     * {@link EnergyCableBlockEntity#distributeEU} as the per-sink call.
     */
    public static long pushEUToSink(BlockEntity neighbour, Direction facing, long voltage, long amperage) {
        IEnergyContainer sink = neighbour.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, facing).orElse(null);
        if (sink == null || !sink.inputsEnergy(facing) || amperage <= 0 || voltage <= 0) return 0;
        return sink.acceptEnergyFromNetwork(facing, voltage, amperage);
    }

    /**
     * Phantom EU producer. Used by the Creative EU Generator so cables can
     * detect it as an EU endpoint and switch into EU mode. The real push
     * still goes through {@link #creativePush} on the generator's tick;
     * this wrapper just exists as a "yes, I speak EU" signal.
     */
    public static LazyOptional<IEnergyContainer> creativeEUProducerCap() {
        return LazyOptional.of(() -> CREATIVE_PRODUCER);
    }

    private static final IEnergyContainer CREATIVE_PRODUCER = new IEnergyContainer() {
        @Override public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) { return 0; }
        @Override public boolean inputsEnergy(Direction side) { return false; }
        @Override public boolean outputsEnergy(Direction side) { return true; }
        @Override public long changeEnergy(long delta) { return 0; }
        @Override public long getEnergyStored() { return Long.MAX_VALUE; }
        @Override public long getEnergyCapacity() { return Long.MAX_VALUE; }
        @Override public long getInputVoltage() { return 0; }
        @Override public long getInputAmperage() { return 0; }
        @Override public long getOutputVoltage() { return 8L; }
        @Override public long getOutputAmperage() { return 1L; }
    };

    /**
     * Tick-side push — called from the BE's tick loop after the Forge FE push
     * fails. Returns the FE amount consumed on a successful push (so the BE
     * can debit its buffer), or 0 if the neighbour doesn't accept EU or has
     * no room. {@code budgetFE} caps the push at the converter's tier
     * throughput ceiling.
     */
    public static int pushToNeighbour(EnergyConverterBlockEntity be, BlockEntity neighbour,
                                      Direction facing, int budgetFE) {
        IEnergyContainer sink = neighbour.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, facing).orElse(null);
        if (sink == null || !sink.inputsEnergy(facing)) return 0;

        // Convert FE budget to EU packets. We push at the GT LV voltage (32)
        // with as many amperage pulses as we can afford from the FE budget.
        long voltage = 32L;
        int feEquivalentPerPacket = (int) voltage * EnergyConverterBlockEntity.FE_PER_EU; // 128 FE
        int maxPackets = budgetFE / feEquivalentPerPacket;
        if (maxPackets <= 0) return 0;

        long amperageAccepted = sink.acceptEnergyFromNetwork(facing, voltage, maxPackets);
        if (amperageAccepted <= 0) return 0;
        return (int) (amperageAccepted * feEquivalentPerPacket);
    }

    // ---------- Adapter class ----------

    private static final class Adapter implements IEnergyContainer {
        private final EnergyConverterBlockEntity be;

        Adapter(EnergyConverterBlockEntity be) {
            this.be = be;
        }

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            // GT sends energy as voltage × amperage packets. Accept as much as
            // our FE buffer can hold, capped by the tier throughput ceiling.
            long maxVoltagePackets = amperage;
            long feBudget = Math.min(be.getTransferRateFE(), be.getCapacityFE() - be.getStoredFE());
            if (feBudget <= 0 || voltage <= 0) return 0;

            long feEquivalentPerPacket = voltage * EnergyConverterBlockEntity.FE_PER_EU;
            if (feEquivalentPerPacket <= 0) return 0;
            long acceptedPackets = Math.min(maxVoltagePackets, feBudget / feEquivalentPerPacket);
            if (acceptedPackets <= 0) return 0;

            int feToAdd = (int) Math.min(Integer.MAX_VALUE, acceptedPackets * feEquivalentPerPacket);
            be.receiveFEInternal(feToAdd);
            return acceptedPackets;
        }

        @Override public boolean inputsEnergy(Direction side) { return true; }
        @Override public boolean outputsEnergy(Direction side) { return true; }

        @Override
        public long changeEnergy(long deltaEU) {
            long deltaFE = deltaEU * EnergyConverterBlockEntity.FE_PER_EU;
            if (deltaFE > 0) {
                int added = be.receiveFEInternal((int) Math.min(Integer.MAX_VALUE, deltaFE));
                return added / EnergyConverterBlockEntity.FE_PER_EU;
            } else if (deltaFE < 0) {
                int removed = be.extractFEInternal((int) Math.min(Integer.MAX_VALUE, -deltaFE));
                return -removed / EnergyConverterBlockEntity.FE_PER_EU;
            }
            return 0;
        }

        @Override public long getEnergyStored() {
            return (long) be.getStoredFE() / EnergyConverterBlockEntity.FE_PER_EU;
        }

        @Override public long getEnergyCapacity() {
            return (long) be.getCapacityFE() / EnergyConverterBlockEntity.FE_PER_EU;
        }

        @Override public long getInputVoltage() { return 32L; }   // LV
        @Override public long getInputAmperage() { return 1L; }
        @Override public long getOutputVoltage() { return 32L; }  // LV
        @Override public long getOutputAmperage() { return 1L; }
    }

    // ---------- Cable adapter ----------
    // Cables in EU mode expose this so producers can push EU into them. The
    // cable doesn't store anything itself — it forwards every accepted packet
    // to consumers walked through the cable network. inputs only (cables
    // don't pull from neighbours; consumers pull / producers push).

    private static final class CableAdapter implements IEnergyContainer {
        private final EnergyCableBlockEntity cable;
        CableAdapter(EnergyCableBlockEntity cable) { this.cable = cable; }

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            return cable.distributeEU(side, voltage, amperage);
        }

        @Override public boolean inputsEnergy(Direction side) { return true; }
        @Override public boolean outputsEnergy(Direction side) { return false; }

        @Override public long changeEnergy(long deltaEU) { return 0L; }
        @Override public long getEnergyStored() { return 0L; }
        @Override public long getEnergyCapacity() { return 0L; }

        // Generous tier — cables don't impose throughput; the bottleneck is
        // the producer's own output voltage and the consumers' input limits.
        @Override public long getInputVoltage()   { return 8192L; } // ZPM
        @Override public long getInputAmperage()  { return Long.MAX_VALUE; }
        @Override public long getOutputVoltage()  { return 0L; }
        @Override public long getOutputAmperage() { return 0L; }
    }
}
