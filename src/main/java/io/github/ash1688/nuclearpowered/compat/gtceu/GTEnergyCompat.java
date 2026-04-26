package io.github.ash1688.nuclearpowered.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
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

    /** Build the LazyOptional the BE hands out when GT asks for EU. */
    public static LazyOptional<IEnergyContainer> createLazy(EnergyConverterBlockEntity be) {
        return LazyOptional.of(() -> new Adapter(be));
    }

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
}
