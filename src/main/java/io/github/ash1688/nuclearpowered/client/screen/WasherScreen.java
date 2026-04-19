package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.WasherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WasherScreen extends AbstractContainerScreen<WasherMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/washer.png");

    // Tank geometry on the GUI (matches the frame painted into the texture).
    private static final int TANK_X = 8;
    private static final int TANK_Y = 17;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 52;

    public WasherScreen(WasherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Water tank fill: blue vertical bar that fills from the bottom up.
        int fluidHeight = menu.getScaledFluid();
        if (fluidHeight > 0) {
            int fillX = x + TANK_X;
            int fillTop = y + TANK_Y + (TANK_HEIGHT - fluidHeight);
            int fillBottom = y + TANK_Y + TANK_HEIGHT;
            g.fill(fillX, fillTop, fillX + TANK_WIDTH, fillBottom, 0xFF2060D0);
        }

        // Progress bar between input and output slots (same style as crusher).
        if (menu.isCrafting()) {
            int filled = menu.getScaledProgress();
            g.fill(x + 78, y + 41, x + 78 + filled, y + 45, 0xFF2080E0);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTankTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    // Tooltip showing "X / Y mB" when hovering the tank.
    private void renderTankTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int left = x + TANK_X;
        int top = y + TANK_Y;
        if (mouseX >= left && mouseX < left + TANK_WIDTH
                && mouseY >= top && mouseY < top + TANK_HEIGHT) {
            Component line = Component.literal(
                    menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB water");
            g.renderTooltip(font, line, mouseX, mouseY);
        }
    }
}
