package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.menu.CladdingRecyclerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CladdingRecyclerScreen extends AbstractContainerScreen<CladdingRecyclerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/cladding_recycler.png");

    private static final int FE_X = 152;
    private static final int FE_Y = 17;
    private static final int FE_WIDTH = 12;
    private static final int FE_HEIGHT = 52;

    private Button autoInputButton;
    private Button autoOutputButton;

    public CladdingRecyclerScreen(CladdingRecyclerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        autoInputButton = Button.builder(autoInputLabel(), b -> sendToggle(CladdingRecyclerMenu.BUTTON_TOGGLE_AUTO_INPUT))
                .bounds(leftPos + 8, topPos + 58, 76, 18).build();
        autoOutputButton = Button.builder(autoOutputLabel(), b -> sendToggle(CladdingRecyclerMenu.BUTTON_TOGGLE_AUTO_OUTPUT))
                .bounds(leftPos + 92, topPos + 58, 76, 18).build();
        addRenderableWidget(autoInputButton);
        addRenderableWidget(autoOutputButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        autoInputButton.setMessage(autoInputLabel());
        autoOutputButton.setMessage(autoOutputLabel());
    }

    private Component autoInputLabel() { return Component.literal("Auto In: " + (menu.isAutoInput() ? "ON" : "OFF")); }
    private Component autoOutputLabel() { return Component.literal("Auto Out: " + (menu.isAutoOutput() ? "ON" : "OFF")); }

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

        drawGhost(g, 36, ModItems.CLADDING_SCRAP.get().getDefaultInstance(), x + 56, y + 35);
        drawGhost(g, 38, ModItems.CLADDING_COMPACTOR.get().getDefaultInstance(), x + 134, y + 35);

        g.fill(x + FE_X - 1, y + FE_Y - 1, x + FE_X + FE_WIDTH + 1, y + FE_Y + FE_HEIGHT + 1, 0xFF555555);
        g.fill(x + FE_X, y + FE_Y, x + FE_X + FE_WIDTH, y + FE_Y + FE_HEIGHT, 0xFF222222);
        int bar = menu.getScaledFE(FE_HEIGHT);
        if (bar > 0) {
            int fillTop = y + FE_Y + (FE_HEIGHT - bar);
            g.fill(x + FE_X, fillTop, x + FE_X + FE_WIDTH, y + FE_Y + FE_HEIGHT, 0xFFE05A20);
        }

        if (menu.isCrafting()) {
            int filled = menu.getScaledProgress();
            g.fill(x + 78, y + 41, x + 78 + filled, y + 45, 0xFFC0C0C0);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderFETooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private void drawGhost(GuiGraphics g, int slotIndex, ItemStack preview, int x, int y) {
        if (!menu.slots.get(slotIndex).getItem().isEmpty()) return;
        g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
        g.renderFakeItem(preview, x, y);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderFETooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (mouseX >= x + FE_X && mouseX < x + FE_X + FE_WIDTH
                && mouseY >= y + FE_Y && mouseY < y + FE_Y + FE_HEIGHT) {
            Component line = Component.literal(menu.getStoredFE() + " / " + menu.getMaxFE() + " FE");
            g.renderTooltip(font, line, mouseX, mouseY);
        }
    }
}
