package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.menu.RpgEquipmentMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Equipment sheet: armor + weapon + offhand + Edit Class.
 */
public class RpgEquipmentScreen extends AbstractContainerScreen<RpgEquipmentMenu> {
    public RpgEquipmentScreen(RpgEquipmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 100;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + 100;
        int y = this.topPos + 8;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.edit_class"), button -> {
            ClassEditScreen.open();
        }).bounds(x, y, 68, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xC0101010);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF8B8B8B);

        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    x + 26,
                    y + 8,
                    x + 74,
                    y + 78,
                    30,
                    0.0625F,
                    mouseX,
                    mouseY,
                    this.minecraft.player
            );
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, -10, 0xFFFFFF, false);
    }
}
