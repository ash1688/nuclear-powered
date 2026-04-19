package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.menu.WasherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WasherScreen extends AbstractContainerScreen<WasherMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/washer.png");

    private static final int TANK_X = 8;
    private static final int TANK_Y = 17;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 52;

    // FE gauge sits between the water tank and the input slot.
    private static final int FE_X = 32;
    private static final int FE_Y = 17;
    private static final int FE_WIDTH = 12;
    private static final int FE_HEIGHT = 52;

    private Button autoInputButton;
    private Button autoOutputButton;

    public WasherScreen(WasherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        // Buttons sit between the slot row and the player inventory, offset right
        // of the tank so they don't clip the water bar.
        autoInputButton = Button.builder(autoInputLabel(), b -> sendToggle(WasherMenu.BUTTON_TOGGLE_AUTO_INPUT))
                .bounds(leftPos + 24, topPos + 58, 68, 18)
                .build();
        autoOutputButton = Button.builder(autoOutputLabel(), b -> sendToggle(WasherMenu.BUTTON_TOGGLE_AUTO_OUTPUT))
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

        int fluidHeight = menu.getScaledFluid();
        if (fluidHeight > 0) {
            int fillX = x + TANK_X;
            int fillTop = y + TANK_Y + (TANK_HEIGHT - fluidHeight);
            int fillBottom = y + TANK_Y + TANK_HEIGHT;
            g.fill(fillX, fillTop, fillX + TANK_WIDTH, fillBottom, 0xFF2060D0);
        }

        g.fill(x + FE_X - 1, y + FE_Y - 1, x + FE_X + FE_WIDTH + 1, y + FE_Y + FE_HEIGHT + 1, 0xFF555555);
        g.fill(x + FE_X, y + FE_Y, x + FE_X + FE_WIDTH, y + FE_Y + FE_HEIGHT, 0xFF222222);
        int bar = menu.getScaledFE(FE_HEIGHT);
        if (bar > 0) {
            int fillTop = y + FE_Y + (FE_HEIGHT - bar);
            int fillBottom = y + FE_Y + FE_HEIGHT;
            g.fill(x + FE_X, fillTop, x + FE_X + FE_WIDTH, fillBottom, 0xFFE05A20);
        }

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
