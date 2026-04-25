package io.github.ash1688.nuclearpowered.compat.gtceu;

import net.minecraftforge.fml.ModList;

/**
 * Central switch for GregTech CEu Modern integration.
 *
 * <p>GT is a <b>soft dependency</b>: NP loads and plays fully without it. Any
 * code path that touches a GT class must sit behind {@link #isLoaded()} so the
 * JVM never tries to link the class when GT isn't on the classpath. Keep this
 * file tiny and classloader-safe — do <b>not</b> import anything from {@code
 * com.gregtechceu.gtceu.*} here.</p>
 *
 * <p>Actual GT API bridges live in sibling classes under this package (e.g.
 * {@code GTEnergyCompat} for the EU side of the FE&lt;-&gt;EU converter). Those
 * classes may safely reference GT types because they're only loaded after a
 * guard check in this class.</p>
 */
public final class GTCompat {
    /** GT CEu Modern's mod id as declared in its own {@code mods.toml}. */
    public static final String MODID = "gtceu";

    private static Boolean cachedLoaded;

    private GTCompat() {}

    /**
     * Whether GT CEu Modern is present in the current runtime. Cached on the
     * first call because {@link ModList} mutates during mod loading; callers
     * from registry events (items, blocks, recipes) need a stable answer.
     */
    public static boolean isLoaded() {
        if (cachedLoaded == null) {
            cachedLoaded = ModList.get().isLoaded(MODID);
        }
        return cachedLoaded;
    }
}
