package io.github.ash1688.nuclearpowered.compat.gtceu;

import com.mojang.logging.LogUtils;
import io.github.ash1688.nuclearpowered.init.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.stream.Collectors;

/**
 * Bridge between NP's own {@code nuclearpowered:steam} fluid and GT CEu's
 * {@code gtceu:steam}. The two are distinct Forge fluid registrations and
 * don't interop out of the box — a NP Coal Boiler emitting NP steam will
 * never fill a GT Steam Macerator, and vice versa.
 *
 * <p>Strategy: when GT is loaded, the NP steam infrastructure (Coal Boiler,
 * Steam Pipe, Steam Engine) silently switches to emitting and transporting
 * GT's steam fluid, so players can power GT's steam machines off an NP
 * Coal Boiler. NP's own steam fluid is still <em>accepted</em> on the
 * consumer side so saves that already have NP steam in the tank don't
 * break after GT is added.</p>
 *
 * <p>When GT is absent, {@link #activeEmitFluid()} falls back to NP's
 * steam and the rest of the module behaves as before.</p>
 *
 * <p>Lookups are lazily cached because the GT fluid registry is only
 * populated after mod construct, but we don't want to repay the
 * {@code ForgeRegistries.FLUIDS.getValue} cost on every boiler tick.</p>
 */
public final class SteamCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GTCEU_STEAM_ID = new ResourceLocation("gtceu", "steam");

    private static volatile Fluid activeEmit;
    private static volatile Fluid cachedGtSteam;
    private static volatile boolean gtLookupMissWarned;

    private SteamCompat() {}

    /**
     * Fluid the NP Coal Boiler should emit each tick. GT steam when GT is
     * loaded; otherwise NP's own steam. Cached after first resolution.
     */
    public static Fluid activeEmitFluid() {
        Fluid a = activeEmit;
        if (a != null) return a;
        synchronized (SteamCompat.class) {
            if (activeEmit != null) return activeEmit;
            Fluid gt = gtSteamOrNull();
            activeEmit = (gt != null) ? gt : ModFluids.STEAM.get();
            return activeEmit;
        }
    }

    /**
     * Whether {@code fluid} is any recognised steam variant that NP's
     * steam-handling machines should accept. True for NP steam, and for
     * GT steam when GT is loaded.
     */
    public static boolean isSteam(Fluid fluid) {
        if (fluid == null) return false;
        if (fluid == ModFluids.STEAM.get()) return true;
        Fluid gt = gtSteamOrNull();
        return gt != null && fluid == gt;
    }

    private static Fluid gtSteamOrNull() {
        if (!GTCompat.isLoaded()) return null;
        Fluid g = cachedGtSteam;
        if (g != null) return g;
        g = ForgeRegistries.FLUIDS.getValue(GTCEU_STEAM_ID);
        cachedGtSteam = g;
        // Defensive: a future GT version could rename or relocate the steam
        // fluid. Keep the warning so a missed lookup is loud rather than
        // silently falling back to NP steam (which GT machines reject).
        if (g == null && !gtLookupMissWarned) {
            gtLookupMissWarned = true;
            String steamFluids = ForgeRegistries.FLUIDS.getKeys().stream()
                    .map(ResourceLocation::toString)
                    .filter(s -> s.toLowerCase().contains("steam"))
                    .sorted()
                    .collect(Collectors.joining(", "));
            LOGGER.warn("gtceu:steam not found in fluid registry. Steam-ish fluids: [{}]", steamFluids);
        }
        return g;
    }
}
