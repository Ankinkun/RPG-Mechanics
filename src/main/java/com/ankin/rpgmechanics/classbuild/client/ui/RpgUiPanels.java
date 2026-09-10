package com.ankin.rpgmechanics.classbuild.client.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class RpgUiPanels {
    private RpgUiPanels() {
    }

    public static void drawFullDim(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, RpgUiTheme.BG_DIM);
        // Soft vignette bands
        graphics.fill(0, 0, width, 40, 0x66000000);
        graphics.fill(0, height - 48, width, height, 0x66000000);
    }

    public static void drawCard(GuiGraphics graphics, int x, int y, int w, int h, boolean accent) {
        graphics.fill(x, y, x + w, y + h, RpgUiTheme.PANEL);
        graphics.renderOutline(x, y, w, h, accent ? RpgUiTheme.ACCENT : RpgUiTheme.PANEL_EDGE);
    }

    public static void drawTabBarBg(GuiGraphics graphics, int width) {
        graphics.fill(0, 0, width, RpgUiTheme.TAB_BAR_H, 0xF00A0C14);
        graphics.fill(0, RpgUiTheme.TAB_BAR_H - 1, width, RpgUiTheme.TAB_BAR_H, RpgUiTheme.PANEL_EDGE_SOFT);
    }
}
