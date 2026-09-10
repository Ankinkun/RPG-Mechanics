package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;

import com.ankin.rpgmechanics.classbuild.CharacterSlot;
import com.ankin.rpgmechanics.classbuild.GearSlot;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgGearUi;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Gear hub: Destiny doll + owned-item bag (stowed ∪ equipped outline). Plain Screen — no grey container sheet.
 */
public class RpgEquipmentScreen extends Screen {
    private static final int BAG_ROWS = 6;

    private int dollLeft;
    private int dollTop;
    private int bagLeft;
    private int bagTop;
    private int scrollRow;

    public RpgEquipmentScreen() {
        super(Component.translatable("screen.rpgmechanics.hub.gear"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.GEAR);
        layout();
    }

    private void layout() {
        int contentTop = RpgUiTheme.TAB_BAR_H + 20;
        this.dollLeft = this.width / 2 - 220;
        this.dollTop = contentTop + 24;
        this.bagLeft = this.width / 2 + 20;
        this.bagTop = contentTop + 40;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        super.render(graphics, mouseX, mouseY, partialTick);
        layout();

        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);

        int dollCardW = 200;
        int dollCardH = 200;
        RpgUiPanels.drawCard(graphics, this.dollLeft - 12, this.dollTop - 16, dollCardW, dollCardH, true);
        graphics.drawString(this.font, this.title, this.dollLeft, this.dollTop - 12, RpgUiTheme.TEXT, false);

        RpgGearUi.drawDoll(
                graphics,
                this.font,
                character,
                this.minecraft != null ? this.minecraft.player : null,
                this.dollLeft,
                this.dollTop,
                mouseX,
                mouseY
        );

        List<RpgGearUi.BagCell> bag = RpgGearUi.buildBag(character);
        int bagW = RpgGearUi.BAG_COLS * (RpgGearUi.SLOT + 4) + 16;
        int bagH = BAG_ROWS * (RpgGearUi.SLOT + 4) + 28;
        RpgUiPanels.drawCard(graphics, this.bagLeft - 8, this.bagTop - 24, bagW, bagH, false);
        graphics.drawString(
                this.font,
                Component.translatable("screen.rpgmechanics.gear.bag"),
                this.bagLeft,
                this.bagTop - 18,
                RpgUiTheme.TEXT_MUTED,
                false
        );
        RpgGearUi.drawBag(graphics, this.font, bag, this.bagLeft, this.bagTop, BAG_ROWS, this.scrollRow);

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.gear.hint"),
                this.width / 2,
                this.height - 24,
                RpgUiTheme.TEXT_MUTED
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);
        GearSlot slot = RpgGearUi.hitEquipSlot(this.dollLeft, this.dollTop, mouseX, mouseY);
        if (slot != null) {
            RpgGearUi.handleDollClick(slot, button);
            return true;
        }
        List<RpgGearUi.BagCell> bag = RpgGearUi.buildBag(character);
        RpgGearUi.BagCell cell = RpgGearUi.hitBag(bag, this.bagLeft, this.bagTop, BAG_ROWS, this.scrollRow, mouseX, mouseY);
        if (cell != null) {
            RpgGearUi.handleBagClick(cell, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);
        int cells = RpgGearUi.buildBag(character).size();
        int maxRow = Math.max(0, (cells + RpgGearUi.BAG_COLS - 1) / RpgGearUi.BAG_COLS - BAG_ROWS);
        if (scrollY > 0) {
            this.scrollRow = Math.max(0, this.scrollRow - 1);
            return true;
        }
        if (scrollY < 0) {
            this.scrollRow = Math.min(maxRow, this.scrollRow + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(new RpgOverviewScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
