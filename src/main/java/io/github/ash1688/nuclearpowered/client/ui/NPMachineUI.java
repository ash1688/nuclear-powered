package io.github.ash1688.nuclearpowered.client.ui;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectAndBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.world.entity.player.Player;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Shared UI builders for NP's machine GUIs.
 *
 * <p>Every machine's {@code createUI(Player)} reproduces the same handful of
 * pieces — vanilla-style background panel, title label, FE bar, processing
 * progress arrow, auto in/out toggle buttons, and the standard player
 * inventory + hotbar layout. Centralising those here keeps each machine's
 * UI builder short and makes restyling trivial: change a colour or a slot
 * position once and every machine picks it up.</p>
 *
 * <p>Layout conventions match what NP's pre-LDLib screens used so JEI overlays
 * and player muscle memory transfer cleanly across the port:
 * <ul>
 *   <li>176x166 panel, 0xC6C6C6 grey fill (vanilla container background).
 *   <li>Title at (8, 6).
 *   <li>Player inventory grid at (8, 84) 3x9, hotbar at (8, 142) 1x9.
 *   <li>FE bar 12x52 at (152, 17), orange fills downward as power drains.
 *   <li>Processing arrow 24x4 between input and output slots.
 *   <li>Auto in/out buttons at (8, 58) and (80, 58), 64x18 each.
 * </ul></p>
 */
public final class NPMachineUI {
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

    /** Vanilla-style grey background covering the whole modular panel. */
    public static void addBackground(WidgetGroup root) {
        root.setBackground(new ColorRectTexture(PANEL_BG));
    }

    /** White drop-shadow title at (8, 6); takes a translation key. */
    public static void addTitle(WidgetGroup root, String langKey) {
        root.addWidget(new LabelWidget(8, 6, langKey)
                .setTextColor(0xFFFFFFFF)
                .setDropShadow(true));
    }

    /** Standard 9x3 player inventory + 9 hotbar at vanilla positions. */
    public static void addPlayerInventory(WidgetGroup root, Player player) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                root.addWidget(new SlotWidget(player.getInventory(), idx,
                        8 + col * 18, 84 + row * 18, true, true));
            }
        }
        for (int col = 0; col < 9; col++) {
            root.addWidget(new SlotWidget(player.getInventory(), col,
                    8 + col * 18, 142, true, true));
        }
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
        return new ProgressWidget(
                () -> capacity == 0 ? 0 : (double) stored.getAsInt() / capacity,
                x, y, 12, 52, tex);
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
                x, y, width, 4, tex);
    }

    /**
     * Auto in/out style toggle button. Label is Supplier-driven so the
     * button text re-renders with "ON" / "OFF" appended each frame as the
     * BE state flips. Standard size 64x18, click runs the supplied action.
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
