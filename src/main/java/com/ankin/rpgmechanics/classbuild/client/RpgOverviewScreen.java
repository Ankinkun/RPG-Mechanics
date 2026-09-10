package com.ankin.rpgmechanics.classbuild.client;

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
 * Character overview hub tab with Destiny doll. Does not pause the world.
 */
public class RpgOverviewScreen extends Screen {
    private int dollLeft;
    private int dollTop;

    public RpgOverviewScreen() {
        super(Component.translatable("screen.rpgmechanics.hub.overview"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.OVERVIEW);
        int cardW = 320;
        int x = (this.width - cardW) / 2;
        this.dollLeft = x + 60;
        this.dollTop = RpgUiTheme.TAB_BAR_H + 56;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        super.render(graphics, mouseX, mouseY, partialTick);

        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);

        int cardW = 320;
        int cardH = 220;
        int x = (this.width - cardW) / 2;
        int y = RpgUiTheme.TAB_BAR_H + 36;
        RpgUiPanels.drawCard(graphics, x, y, cardW, cardH, true);

        String name = character.occupied() ? character.name() : "—";
        graphics.drawCenteredString(this.font, name, this.width / 2, y + 12, RpgUiTheme.TEXT);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.darkness_dd"),
                this.width / 2,
                y + 28,
                RpgUiTheme.ACCENT
        );

        this.dollLeft = x + 60;
        this.dollTop = y + 40;
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

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.hub.overview_hint"),
                this.width / 2,
                this.height - 28,
                RpgUiTheme.TEXT_MUTED
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GearSlot slot = RpgGearUi.hitEquipSlot(this.dollLeft, this.dollTop, mouseX, mouseY);
        if (slot != null) {
            RpgGearUi.handleDollClick(slot, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
