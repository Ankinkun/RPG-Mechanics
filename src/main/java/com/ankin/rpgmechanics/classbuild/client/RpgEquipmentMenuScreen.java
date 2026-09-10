package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.menu.RpgEquipmentMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Legacy container screen — immediately replaces itself with the Destiny gear Screen.
 * Kept so {@link net.neoforged.neoforge.client.event.RegisterMenuScreensEvent} stays valid.
 */
public class RpgEquipmentMenuScreen extends AbstractContainerScreen<RpgEquipmentMenu> {
    public RpgEquipmentMenuScreen(RpgEquipmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            this.minecraft.setScreen(new RpgEquipmentScreen());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }
}
