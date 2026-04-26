package io.github.ash1688.nuclearpowered.client.ui;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectAndBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Shared UI builders for NP's machine GUIs.
 *
 * <p>Every machine's {@code createUI(Player)} reproduces the same handful of
 * pieces — vanilla-style background panel, title label, FE bar, processing
 * progress arrow, and the standard player inventory + hotbar layout.
 * Centralising those here keeps each machine's UI builder short and makes
 * restyling trivial: change a colour or a slot position once and every
 * machine picks it up.</p>
 *
 * <p>Layout: every machine UI is rendered as a 200×166 ModularUI. The
 * leftmost {@link #TABS_W} pixels (24 px) are reserved for left-edge tab
 * buttons (I/O, side config, redstone, etc.); the next {@link #PANEL_W}
 * pixels (176) host the original 176×166 vanilla-shaped panel. All slot/
 * widget positions passed to the helpers below are panel-relative —
 * the helpers add {@link #PANEL_X} (= TABS_W) automatically, so machine
 * code reads the same as before the tab strip was added.</p>
 */
public final class NPMachineUI {
    /** Width of the left-edge tab strip. */
    public static final int TABS_W = 24;
    /** Inner panel width — matches the original vanilla 176×166 GUI. */
    public static final int PANEL_W = 176;
    public static final int PANEL_H = 166;
    /** Total ModularUI width: tabs strip + panel. */
    public static final int UI_W = TABS_W + PANEL_W;
    public static final int UI_H = PANEL_H;
    /** X offset where the inner panel starts (= TABS_W). All slot/widget
     *  positions in helpers are panel-relative; helpers add this offset. */
    public static final int PANEL_X = TABS_W;

    /** Vanilla container background grey. Matches every other Minecraft GUI. */
    public static final int PANEL_BG = 0xFFC6C6C6;

    /** FE bar: dark grey "empty" segment behind the orange fill. */
    public static final int FE_BAR_EMPTY = 0xFF222222;
    /** FE bar: orange "filled" colour — same hue NP used pre-LDLib. */
    public static final int FE_BAR_FULL = 0xFFE05A20;

    /** Processing arrow: dark grey empty, slightly redder orange when active. */
    public static final int PROGRESS_BAR_EMPTY = 0xFF222222;
    public static final int PROGRESS_BAR_FULL = 0xFFE56B2B;

    /** Toggle button frame: medium grey fill with darker border. */
    public static final int BUTTON_FILL = 0xFF8B8B8B;
    public static final int BUTTON_BORDER = 0xFF373737;

    private NPMachineUI() {}

    /** Vanilla-style grey background covering the inner panel only (not the tab strip). */
    public static void addBackground(WidgetGroup root) {
        root.addWidget(new ImageWidget(PANEL_X, 0, PANEL_W, PANEL_H,
                new ColorRectTexture(PANEL_BG)));
    }

    /** White drop-shadow title at panel-(8, 6); takes a translation key. */
    public static void addTitle(WidgetGroup root, String langKey) {
        root.addWidget(new LabelWidget(PANEL_X + 8, 6, langKey)
                .setTextColor(0xFFFFFFFF)
                .setDropShadow(true));
    }

    /** Standard 9x3 player inventory + 9 hotbar at vanilla panel-relative positions. */
    public static void addPlayerInventory(WidgetGroup root, Player player) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                root.addWidget(new SlotWidget(player.getInventory(), idx,
                        PANEL_X + 8 + col * 18, 84 + row * 18, true, true));
            }
        }
        for (int col = 0; col < 9; col++) {
            root.addWidget(new SlotWidget(player.getInventory(), col,
                    PANEL_X + 8 + col * 18, 142, true, true));
        }
    }

    /** Machine slot at panel-relative (x, y), offset to absolute coords. */
    public static SlotWidget slot(IItemTransfer handler, int slotIdx, int x, int y,
                                   boolean canPut, boolean canTake) {
        return new SlotWidget(handler, slotIdx, PANEL_X + x, y, canPut, canTake);
    }

    /** Slot bound to a vanilla {@link Container} (player inv etc.). */
    public static SlotWidget slot(Container container, int slotIdx, int x, int y,
                                   boolean canPut, boolean canTake) {
        return new SlotWidget(container, slotIdx, PANEL_X + x, y, canPut, canTake);
    }

    /**
     * Vertical FE bar 12x52. Orange fills from the bottom up as energy
     * accumulates (conventional liquid-tank look).
     */
    public static ProgressWidget feBar(int x, int y, IntSupplier stored, int capacity) {
        ProgressTexture tex = new ProgressTexture(
                new ColorRectTexture(FE_BAR_EMPTY),
                new ColorRectTexture(FE_BAR_FULL))
                .setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
        ProgressWidget bar = new ProgressWidget(
                () -> capacity == 0 ? 0 : (double) stored.getAsInt() / capacity,
                PANEL_X + x, y, 12, 52, tex);
        bar.setDynamicHoverTips(frac -> stored.getAsInt() + " / " + capacity + " FE");
        return bar;
    }

    /**
     * Horizontal processing-progress arrow. Dark empty, orange fill that
     * grows left-to-right as the recipe completes. Default size matches
     * NP's pre-LDLib arrow at 24x4.
     */
    public static ProgressWidget progressArrow(int x, int y, int width,
                                                IntSupplier progress,
                                                IntSupplier maxProgress) {
        ProgressTexture tex = new ProgressTexture(
                new ColorRectTexture(PROGRESS_BAR_EMPTY),
                new ColorRectTexture(PROGRESS_BAR_FULL))
                .setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        return new ProgressWidget(
                () -> {
                    int max = maxProgress.getAsInt();
                    return max == 0 ? 0 : (double) progress.getAsInt() / max;
                },
                PANEL_X + x, y, width, 4, tex);
    }

    /**
     * Vertical fluid tank, 16x52 by default. Wraps a Forge {@link IFluidHandler}
     * into LDLib's {@link IFluidTransfer} and renders the bucket-level texture
     * + click-to-fill / click-to-drain hover overlay so players can move fluids
     * with held buckets just like vanilla. Hover tooltip shows fluid name + amount
     * automatically; no extra label widgets needed.
     */
    public static TankWidget tankBar(int x, int y, int width, int height,
                                      IFluidHandler tank) {
        IFluidTransfer wrapped = FluidTransferHelperImpl.toFluidTransfer(tank);
        return new TankWidget(wrapped, 0, PANEL_X + x, y, width, height, true, true)
                .setShowAmount(true)
                .setDrawHoverTips(true);
    }

    /** Default 16x52 tank at the given panel-relative position. */
    public static TankWidget tankBar(int x, int y, IFluidHandler tank) {
        return tankBar(x, y, 16, 52, tank);
    }

    /**
     * Auto in/out style toggle button. Label is Supplier-driven so the
     * button text re-renders with "ON" / "OFF" appended each frame as the
     * BE state flips. Standard size 64x18, click runs the supplied action.
     *
     * <p>Position is given in absolute coords (no PANEL_X offset) — toggle
     * buttons are now used inside tab content panels, where the caller
     * already owns the layout space.</p>
     */
    public static Widget toggleButton(int x, int y, int width, String label,
                                       BooleanSupplier state,
                                       Runnable onClick) {
        IGuiTexture tex = new GuiTextureGroup(
                new ColorRectAndBorderTexture(BUTTON_FILL, BUTTON_BORDER, 1f),
                new TextTexture(() -> label + ": " + (state.getAsBoolean() ? "ON" : "OFF"))
                        .setColor(0xFFFFFFFF)
                        .setDropShadow(true));
        return new ButtonWidget(x, y, width, 18, tex, cd -> onClick.run())
                .setHoverTooltips("Click to toggle " + label.toLowerCase());
    }
}
