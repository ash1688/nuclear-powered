package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.SteamEngineMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SteamEngineScreen extends AbstractContainerScreen<SteamEngineMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/steam_engine.png");

    private static final int STEAM_X = 26, STEAM_Y = 17, TANK_W = 12, TANK_H = 52;
    private static final int FE_X = 136, FE_Y = 17;

    public SteamEngineScreen(SteamEngineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int sBar = menu.getScaledSteam(TANK_H);
        if (sBar > 0) {
            g.fill(x + STEAM_X, y + STEAM_Y + (TANK_H - sBar),
                   x + STEAM_X + TANK_W, y + STEAM_Y + TANK_H, 0xFFC0C0D0);
        }

        int feBar = menu.getScaledFE(TANK_H);
        if (feBar > 0) {
            g.fill(x + FE_X, y + FE_Y + (TANK_H - feBar),
                   x + FE_X + TANK_W, y + FE_Y + TANK_H, 0xFFE05A20);
        }

        // "Generating Nfe/t" indicator in the centre.
        Font font = this.font;
        String status = menu.getLastFEGenerated() > 0
                ? "Running: " + menu.getLastFEGenerated() + " FE/t"
                : "Idle";
        int color = menu.getLastFEGenerated() > 0 ? 0xFF2F7F2F : 0xFF888888;
        g.drawString(font, status, x + 60, y + 36, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTankTooltips(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderTankTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int sx = x + STEAM_X, sy = y + STEAM_Y;
        if (mouseX >= sx && mouseX < sx + TANK_W && mouseY >= sy && mouseY < sy + TANK_H) {
            g.renderTooltip(font, Component.literal(
                    menu.getSteamAmount() + " / " + menu.getSteamCapacity() + " mB steam"),
                    mouseX, mouseY);
        }
        int fx = x + FE_X, fy = y + FE_Y;
        if (mouseX >= fx && mouseX < fx + TANK_W && mouseY >= fy && mouseY < fy + TANK_H) {
            g.renderTooltip(font, Component.literal(
                    menu.getStoredFE() + " / " + menu.getMaxFE() + " FE"),
                    mouseX, mouseY);
        }
    }
}
