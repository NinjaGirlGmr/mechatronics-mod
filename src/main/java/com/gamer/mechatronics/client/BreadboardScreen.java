package com.gamer.mechatronics.client;

import com.gamer.mechatronics.menu.BreadboardMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BreadboardScreen extends AbstractContainerScreen<BreadboardMenu> {

    public BreadboardScreen(BreadboardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);  // window size in pixels
    }

    // Draws the window background
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        // dark border, then a light gray panel inside it
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6C6C6);
    }

    // Draws text. Only the title; we skip the default "Inventory" label.
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }
}