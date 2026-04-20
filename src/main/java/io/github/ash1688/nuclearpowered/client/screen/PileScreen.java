package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.PileMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class PileScreen extends AbstractContainerScreen<PileMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/graphite_pile.png");

    // Heat gauge geometry — same frame the GUI texture paints.
    private static final int HEAT_X = 8;
    private static final int HEAT_Y = 17;
    private static final int HEAT_WIDTH = 12;
    private static final int HEAT_HEIGHT = 52;

    private Button autoInputButton;
    private Button autoOutputButton;

    public PileScreen(PileMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        autoInputButton = Button.builder(autoInputLabel(), b -> sendToggle(PileMenu.BUTTON_TOGGLE_AUTO_INPUT))
                .bounds(leftPos + 24, topPos + 58, 68, 18)
                .build();
        autoOutputButton = Button.builder(autoOutputLabel(), b -> sendToggle(PileMenu.BUTTON_TOGGLE_AUTO_OUTPUT))
                .bounds(leftPos + 96, topPos + 58, 68, 18)
                .build();
        addRenderableWidget(autoInputButton);
        addRenderableWidget(autoOutputButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        autoInputButton.setMessage(autoInputLabel());
        autoOutputButton.setMessage(autoOutputLabel());
    }

    private Component autoInputLabel() {
        return Component.literal("Auto In: " + (menu.isAutoInput() ? "ON" : "OFF"));
    }

    private Component autoOutputLabel() {
        return Component.literal("Auto Out: " + (menu.isAutoOutput() ? "ON" : "OFF"));
    }

    private void sendToggle(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Heat gauge — colour ramps from yellow-green (cool) through orange (warning)
        // to red (critical) as heat rises.
        int heatBar = menu.getScaledHeat();
        if (heatBar > 0) {
            int fillX = x + HEAT_X;
            int fillTop = y + HEAT_Y + (HEAT_HEIGHT - heatBar);
            int fillBottom = y + HEAT_Y + HEAT_HEIGHT;
            int intensity = Math.min(255, menu.getHeat() * 255 / Math.max(1, menu.getMaxHeat()));
            int red = 0x40 + intensity * 0xA0 / 255;
            int green = 0xC0 - intensity * 0xA0 / 255;
            int color = 0xFF000000 | (red << 16) | (green << 8) | 0x20;
            g.fill(fillX, fillTop, fillX + HEAT_WIDTH, fillBottom, color);
        }

        // Burn progress arrow between the two slots.
        if (menu.isBurning()) {
            int filled = menu.getScaledBurnProgress();
            g.fill(x + 78, y + 41, x + 78 + filled, y + 45, 0xFFE56B2B);
        }

        // Red banner when the 3x3x3 shell is missing — surfaces the problem without
        // requiring the player to hover the heat gauge for the tooltip.
        if (!menu.isStructureValid()) {
            String msg = "Structure Incomplete";
            int w = font.width(msg);
            g.drawString(font, msg, x + (imageWidth - w) / 2, y + 23, 0xFFCC3333, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderHeatTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderHeatTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int left = x + HEAT_X;
        int top = y + HEAT_Y;
        if (mouseX >= left && mouseX < left + HEAT_WIDTH
                && mouseY >= top && mouseY < top + HEAT_HEIGHT) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(
                    "Heat: " + menu.getHeat() + " / " + menu.getMaxHeat()).getVisualOrderText());
            if (menu.isStructureValid()) {
                lines.add(Component.literal(
                        "Structure: Complete (" + menu.getCasingCount() + " casings)").getVisualOrderText());
            } else {
                lines.add(Component.literal("Structure: Incomplete").getVisualOrderText());
                lines.add(Component.literal(
                        "Needs a full 3x3x3 graphite casing shell").getVisualOrderText());
            }
            int delta = menu.getHeatDelta();
            if (delta != 0) {
                String sign = delta > 0 ? "+" : "";
                lines.add(Component.literal("Heat rate: " + sign + delta + " / sec").getVisualOrderText());
            }
            g.renderTooltip(font, lines, mouseX, mouseY);
        }
    }
}
