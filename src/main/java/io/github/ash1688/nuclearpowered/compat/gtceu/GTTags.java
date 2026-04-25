package io.github.ash1688.nuclearpowered.compat.gtceu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Typed constants for the GT CEu / common Forge material tags Nuclear Powered
 * consumes in tier-gated crafting recipes.
 *
 * <p>Centralising these in one file means if GT or the Forge tag convention
 * drifts (as happened between 1.19 and 1.20 for several of these), we update
 * exactly one place rather than every recipe JSON and every Java reference.
 * The JSON side already uses these paths directly — this class is for Java
 * code (tier gates, ingredient builders, JEI hints).</p>
 *
 * <p>Tag paths follow the {@code forge:<category>/<material>} convention
 * because that's what GT CEu publishes; cross-mod compatibility falls out
 * naturally via {@code replace: false} tag merges.</p>
 */
public final class GTTags {
    private GTTags() {}

    // ---- Circuits (tier-keyed, used as the tier gate on NP machine recipes) ----

    /** Basic LV circuit tag — T1 NP machine crafting, first-tier content. */
    public static final TagKey<Item> CIRCUITS_LV = itemTag("circuits/lv");
    /** MV circuit — unlocks T2 reactors (Magnox / RBMK). */
    public static final TagKey<Item> CIRCUITS_MV = itemTag("circuits/mv");
    /** HV circuit — T3 reactors and advanced reprocessing. */
    public static final TagKey<Item> CIRCUITS_HV = itemTag("circuits/hv");
    /** EV circuit — T4 fast breeder / MSR / LFTR. */
    public static final TagKey<Item> CIRCUITS_EV = itemTag("circuits/ev");
    /** IV circuit — T5 SMR endgame controller. */
    public static final TagKey<Item> CIRCUITS_IV = itemTag("circuits/iv");

    // ---- Structural metals (tag-compatible with GT / Mekanism / Create) ----

    public static final TagKey<Item> PLATES_IRON = itemTag("plates/iron");
    public static final TagKey<Item> PLATES_STEEL = itemTag("plates/steel");
    public static final TagKey<Item> PLATES_ALUMINIUM = itemTag("plates/aluminium");
    public static final TagKey<Item> PLATES_ZIRCONIUM = itemTag("plates/zirconium");
    public static final TagKey<Item> PLATES_TUNGSTEN = itemTag("plates/tungsten");

    public static final TagKey<Item> INGOTS_STEEL = itemTag("ingots/steel");
    public static final TagKey<Item> INGOTS_ALUMINIUM = itemTag("ingots/aluminium");
    public static final TagKey<Item> INGOTS_TUNGSTEN = itemTag("ingots/tungsten");

    public static final TagKey<Item> WIRES_COPPER = itemTag("wires/copper");
    public static final TagKey<Item> WIRES_TIN = itemTag("wires/tin");
    public static final TagKey<Item> WIRES_GOLD = itemTag("wires/gold");

    // ---- Fluid-transport parts (for reprocessing machine recipes) ----

    public static final TagKey<Item> PIPES_STEEL = itemTag("pipes/fluid/steel");
    public static final TagKey<Item> PIPES_STAINLESS = itemTag("pipes/fluid/stainless_steel");

    // ---- Machine parts (only exist when GT is loaded; referenced by the Assembler) ----

    public static final TagKey<Item> HULLS_LV = itemTag("machine_hulls/lv");
    public static final TagKey<Item> HULLS_MV = itemTag("machine_hulls/mv");
    public static final TagKey<Item> HULLS_HV = itemTag("machine_hulls/hv");

    // ---- Block tags (used sparingly — most NP recipes consume items) ----

    public static final TagKey<Block> BLOCKS_REINFORCED_CONCRETE = blockTag("reinforced_concrete");

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                new ResourceLocation("forge", path));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                new ResourceLocation("forge", path));
    }
}
