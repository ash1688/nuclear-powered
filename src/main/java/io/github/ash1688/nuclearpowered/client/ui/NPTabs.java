package io.github.ash1688.nuclearpowered.client.ui;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectAndBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.ash1688.nuclearpowered.energy.EnergyMode;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the left-edge tab strip shared across every NP machine UI.
 *
 * <p>Layout: tab buttons stack vertically along the leftmost
 * {@link NPMachineUI#TABS_W} pixels of the ModularUI. Each button is bound to
 * a content {@link WidgetGroup} positioned over the main 176×166 panel; when
 * its button is selected the group renders on top, when deselected it's
 * hidden by the {@link TabContainer}.</p>
 *
 * <p>The first tab added is "Main" — its content group is empty so the main
 * UI (slots, FE bar, etc.) is visible by default. Switching to any other tab
 * overlays its config page; clicking "Main" again hides the page.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   ui.mainGroup.addWidget(new NPTabs()
 *       .ioTab(() -> autoInput, this::toggleAutoInput,
 *              () -> autoOutput, this::toggleAutoOutput)
 *       .build());
 * }</pre></p>
 */
public final class NPTabs {
    /** Each button is 24×22 with 2 px gap between them. */
    private static final int BTN_W = 24;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 2;
    /** First button starts a few px below the top edge. */
    private static final int BTN_FIRST_Y = 4;

    private final TabContainer container;
    private int nextBtnY = BTN_FIRST_Y;

    public NPTabs() {
        container = new TabContainer(0, 0, NPMachineUI.UI_W, NPMachineUI.UI_H);
        // Default tab: empty content panel — selecting it hides any other
        // active tab and reveals the main UI underneath. This is what the
        // player sees on first open.
        WidgetGroup empty = new WidgetGroup(0, 0, NPMachineUI.UI_W, NPMachineUI.UI_H);
        container.addTab(makeButton("Main"), empty);
    }

    /**
     * I/O config page — Auto In and Auto Out toggles. Replaces the inline
     * buttons every machine used to render at (8, 58) and (80, 58).
     */
    public NPTabs ioTab(BooleanSupplier autoIn, Runnable toggleIn,
                         BooleanSupplier autoOut, Runnable toggleOut) {
        WidgetGroup content = panelOverlay();
        content.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 6, "I/O")
                .setTextColor(0xFFFFFFFF)
                .setDropShadow(true));
        content.addWidget(NPMachineUI.toggleButton(NPMachineUI.PANEL_X + 8, 28, 80,
                "Auto In", autoIn, toggleIn));
        content.addWidget(NPMachineUI.toggleButton(NPMachineUI.PANEL_X + 8, 52, 80,
                "Auto Out", autoOut, toggleOut));
        container.addTab(makeButton("I/O"), content);
        return this;
    }

    /**
     * Energy-mode tab. Shows the current mode (FE / EU) and a toggle button.
     * Toggle is gated by {@code canToggle} — typically returns false while the
     * machine is processing so a mid-cycle switch can't lose work, and false
     * for FE→EU when GTCEU isn't loaded.
     */
    public NPTabs energyTab(Supplier<EnergyMode> mode, Runnable toggle, BooleanSupplier canToggle) {
        WidgetGroup content = panelOverlay();
        content.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 6, "Energy")
                .setTextColor(0xFFFFFFFF)
                .setDropShadow(true));
        content.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 28,
                () -> "§fMode: §a" + mode.get().displayUnit())
                .setDropShadow(true));
        content.addWidget(new LabelWidget(NPMachineUI.PANEL_X + 8, 40,
                () -> canToggle.getAsBoolean()
                        ? "§7Click below to switch system."
                        : "§cBusy — finish current process first.")
                .setDropShadow(true));
        IGuiTexture switchTex = new GuiTextureGroup(
                new ColorRectAndBorderTexture(NPMachineUI.BUTTON_FILL,
                        NPMachineUI.BUTTON_BORDER, 1f),
                new TextTexture(() -> "Switch to " + mode.get().opposite().displayUnit())
                        .setColor(0xFFFFFFFF)
                        .setDropShadow(true));
        content.addWidget(new ButtonWidget(NPMachineUI.PANEL_X + 8, 60, 80, 18,
                switchTex, cd -> { if (canToggle.getAsBoolean()) toggle.run(); }));
        container.addTab(makeButton("EM"), content);
        return this;
    }

    /** Returns the {@link TabContainer} for adding to the ModularUI's mainGroup. */
    public TabContainer build() {
        return container;
    }

    /**
     * Stacks the next button at {@link #nextBtnY} along the left edge,
     * advancing the cursor for the following call.
     */
    private TabButton makeButton(String label) {
        IGuiTexture base = new GuiTextureGroup(
                new ColorRectAndBorderTexture(NPMachineUI.BUTTON_FILL,
                        NPMachineUI.BUTTON_BORDER, 1f),
                new TextTexture(label)
                        .setColor(0xFFFFFFFF)
                        .setDropShadow(true));
        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectAndBorderTexture(0xFFA8A8A8,
                        NPMachineUI.BUTTON_BORDER, 1f),
                new TextTexture(label)
                        .setColor(0xFFFFFFFF)
                        .setDropShadow(true));
        IGuiTexture pressed = new GuiTextureGroup(
                new ColorRectAndBorderTexture(0xFFD8D8D8,
                        NPMachineUI.BUTTON_BORDER, 1f),
                new TextTexture(label)
                        .setColor(0xFF202020));
        TabButton btn = new TabButton(0, nextBtnY, BTN_W, BTN_H)
                .setBaseTexture(base)
                .setHoverTexture(hover)
                .setPressedTexture(pressed);
        nextBtnY += BTN_H + BTN_GAP;
        return btn;
    }

    /**
     * Tab content panel that sits over the main 176×166 panel area, with
     * the same background colour so an active tab visually replaces the
     * machine view.
     */
    private WidgetGroup panelOverlay() {
        WidgetGroup g = new WidgetGroup(NPMachineUI.PANEL_X, 0,
                NPMachineUI.PANEL_W, NPMachineUI.PANEL_H);
        g.addWidget(new ImageWidget(0, 0, NPMachineUI.PANEL_W, NPMachineUI.PANEL_H,
                new ColorRectTexture(NPMachineUI.PANEL_BG)));
        return g;
    }
}
