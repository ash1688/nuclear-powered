package io.github.ash1688.nuclearpowered.energy;

/**
 * Per-block energy system selector. A machine is either in FE mode (Forge
 * Energy, the cross-mod default) or EU mode (GregTech CEu Voltage × Amperage).
 *
 * <p>Cables, producers, storers and consumers all carry a mode flag. When two
 * neighbours are in matching modes they connect and exchange energy normally;
 * when they're in mismatching modes the connection is refused entirely (the
 * cable's render face simply doesn't link). EU mode is only usable when GTCEU
 * is loaded — without it, the toggle is silently rejected.</p>
 *
 * <p>Internally every BE stores energy in FE units. The {@link #FE_PER_EU}
 * ratio (= 4, matching GT CEu's canonical 1 EU = 4 FE) lets EU-mode buffers
 * display and exchange in EU units without losing precision; the underlying
 * FE count is unchanged across mode switches, so a half-full battery in FE
 * mode is still half-full when toggled to EU.</p>
 */
public enum EnergyMode {
    FE,
    EU;

    /** GT CEu canonical conversion ratio. */
    public static final int FE_PER_EU = 4;

    public EnergyMode opposite() {
        return this == FE ? EU : FE;
    }

    public String displayUnit() {
        return this == FE ? "FE" : "EU";
    }

    /** Convert an FE-denominated amount into this mode's display unit. */
    public int displayFromFE(int feAmount) {
        return this == FE ? feAmount : feAmount / FE_PER_EU;
    }
}
