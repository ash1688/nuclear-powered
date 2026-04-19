package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.BatteryMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/battery.png");

    private static final int FE_X = 8;
    private static final int FE_Y = 17;
    private static final int FE_WIDTH = 12;
    private static final int FE_HEIGHT = 52;

    public BatteryScreen(BatteryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int bar = menu.getScaledFE(FE_HEIGHT);
        if (bar > 0) {
            int fillX = x + FE_X;
            int fillTop = y + FE_Y + (FE_HEIGHT - bar);
            int fillBottom = y + FE_Y + FE_HEIGHT;
            g.fill(fillX, fillTop, fillX + FE_WIDTH, fillBottom, 0xFFE05A20);
        }

        // Status panel: exact numeric values + percentage.
        Font font = this.font;
        int panelX = x + 32;
        int panelY = y + 22;
        int stored = menu.getStoredFE();
        int max = menu.getMaxFE();
        int percent = max == 0 ? 0 : stored * 100 / max;
        g.drawString(font, "Stored: " + stored + " FE", panelX, panelY, 0xFF303030, false);
        g.drawString(font, "Capacity: " + max + " FE", panelX, panelY + 12, 0xFF303030, false);
        g.drawString(font, "Charge: " + percent + "%", panelX, panelY + 24, 0xFF404040, false);
        g.drawString(font, "I/O: " + 1024 + " FE/t", panelX, panelY + 36, 0xFF606060, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderFETooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderFETooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int left = x + FE_X;
        int top = y + FE_Y;
        if (mouseX >= left && mouseX < left + FE_WIDTH
                && mouseY >= top && mouseY < top + FE_HEIGHT) {
            Component line = Component.literal(
                    menu.getStoredFE() + " / " + menu.getMaxFE() + " FE");
            g.renderTooltip(font, line, mouseX, mouseY);
        }
    }
}
