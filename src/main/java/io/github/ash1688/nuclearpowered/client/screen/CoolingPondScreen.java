package io.github.ash1688.nuclearpowered.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ash1688.nuclearpowered.NuclearPowered;
import io.github.ash1688.nuclearpowered.init.ModItems;
import io.github.ash1688.nuclearpowered.menu.CoolingPondMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CoolingPondScreen extends AbstractContainerScreen<CoolingPondMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NuclearPowered.MODID, "textures/gui/container/cooling_pond.png");

    public CoolingPondScreen(CoolingPondMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Ghost on empty input slot (rest of slots handled by vanilla rendering).
        if (menu.slots.get(36).getItem().isEmpty()) {
            g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.renderFakeItem(new ItemStack(ModItems.HOT_SPENT_FUEL_ROD.get()), x + 44, y + 35);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // Horizontal progress bar between input (left) and cooling (right) slots.
        int filled = menu.getScaledProgress(52);
        if (filled > 0) {
            g.fill(x + 62, y + 40, x + 62 + filled, y + 44, 0xFF4090E0);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        // Tooltip over the progress bar
        if (mouseX >= x + 62 && mouseX < x + 114 && mouseY >= y + 40 && mouseY < y + 44) {
            int p = menu.getCoolProgress();
            int m = menu.getCoolTicks();
            int remainingSec = Math.max(0, (m - p) / 20);
            g.renderTooltip(font, Component.literal("Cooling: " + remainingSec + "s remaining"), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
