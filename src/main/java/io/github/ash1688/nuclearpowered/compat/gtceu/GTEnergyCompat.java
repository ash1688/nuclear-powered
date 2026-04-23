package io.github.ash1688.nuclearpowered.compat.gtceu;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.mojang.logging.LogUtils;
import io.github.ash1688.nuclearpowered.block.converter.EnergyConverterBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

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
    private static final Logger LOGGER = LogUtils.getLogger();
    // Log once per direction — enough to diagnose whether any GT sink was
    // found and whether it actually accepts the offer.
    private static final ConcurrentHashMap<Direction, Boolean> PUSH_LOGGED = new ConcurrentHashMap<>();

    private GTEnergyCompat() {}

    /** Capability-token match test — used by the BE's getCapability router. */
    public static boolean isEnergyContainerCap(Capability<?> cap) {
        return cap == GTCapability.CAPABILITY_ENERGY_CONTAINER;
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
        boolean firstForDir = !Boolean.TRUE.equals(PUSH_LOGGED.put(facing, Boolean.TRUE));
        if (sink == null) {
            if (firstForDir) {
                LOGGER.info("[NP/GT-EU] Push dir={} neighbourClass={} — no IEnergyContainer capability",
                        facing, neighbour.getClass().getName());
            }
            return 0;
        }
        if (!sink.inputsEnergy(facing)) {
            if (firstForDir) {
                LOGGER.info("[NP/GT-EU] Push dir={} neighbourClass={} — sink refuses inputsEnergy on this face",
                        facing, neighbour.getClass().getName());
            }
            return 0;
        }

        // Convert FE budget to EU packets. We push at the GT LV voltage (32)
        // with as many amperage pulses as we can afford from the FE budget.
        long voltage = 32L;
        int feEquivalentPerPacket = (int) voltage * EnergyConverterBlockEntity.FE_PER_EU; // 128 FE
        int maxPackets = budgetFE / feEquivalentPerPacket;
        if (maxPackets <= 0) {
            if (firstForDir) {
                LOGGER.info("[NP/GT-EU] Push dir={} budgetFE={} < feEquivalentPerPacket={}; nothing to send",
                        facing, budgetFE, feEquivalentPerPacket);
            }
            return 0;
        }

        long amperageAccepted = sink.acceptEnergyFromNetwork(facing, voltage, maxPackets);
        if (firstForDir) {
            LOGGER.info("[NP/GT-EU] Push dir={} neighbourClass={} voltage={} maxPackets={} -> acceptedPackets={}",
                    facing, neighbour.getClass().getName(), voltage, maxPackets, amperageAccepted);
        }
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
