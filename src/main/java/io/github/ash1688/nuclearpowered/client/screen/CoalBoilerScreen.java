package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.CoalBoilerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CoalBoilerScreen extends AbstractContainerScreen<CoalBoilerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/coal_boiler.png");

    // Tank geometry matches the frames painted into the GUI texture.
    private static final int WATER_X = 26, WATER_Y = 17, TANK_W = 12, TANK_H = 52;
    private static final int STEAM_X = 136, STEAM_Y = 17;

    public CoalBoilerScreen(CoalBoilerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Water tank (blue).
        int wBar = menu.getScaledWater(TANK_H);
        if (wBar > 0) {
            g.fill(x + WATER_X, y + WATER_Y + (TANK_H - wBar),
                   x + WATER_X + TANK_W, y + WATER_Y + TANK_H, 0xFF2060D0);
        }

        // Steam tank (light grey).
        int sBar = menu.getScaledSteam(TANK_H);
        if (sBar > 0) {
            g.fill(x + STEAM_X, y + STEAM_Y + (TANK_H - sBar),
                   x + STEAM_X + TANK_W, y + STEAM_Y + TANK_H, 0xFFC0C0D0);
        }

        // Flame indicator between the fuel slot and the bucket slot when burning.
        if (menu.isBurning()) {
            int maxBurn = menu.getMaxBurnTime();
            int burnPixels = maxBurn == 0 ? 0 : menu.getBurnTime() * 13 / maxBurn;
            int fx = x + 81;
            int fy = y + 46;
            g.fill(fx, fy - burnPixels, fx + 14, fy, 0xFFE0842B);
        }
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
        int wx = x + WATER_X, wy = y + WATER_Y;
        if (mouseX >= wx && mouseX < wx + TANK_W && mouseY >= wy && mouseY < wy + TANK_H) {
            g.renderTooltip(font, Component.literal(
                    menu.getWaterAmount() + " / " + menu.getWaterCapacity() + " mB water"),
                    mouseX, mouseY);
        }
        int sx = x + STEAM_X, sy = y + STEAM_Y;
        if (mouseX >= sx && mouseX < sx + TANK_W && mouseY >= sy && mouseY < sy + TANK_H) {
            g.renderTooltip(font, Component.literal(
                    menu.getSteamAmount() + " / " + menu.getSteamCapacity() + " mB steam"),
                    mouseX, mouseY);
        }
    }
}
