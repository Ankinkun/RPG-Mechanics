package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

/**
 * Hard Xaero World Map client bridge (compileOnly).
 */
public final class XaeroWorldMapClientBridge {
    private XaeroWorldMapClientBridge() {
    }

    public static boolean openWorldMap() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable() || session.getMapProcessor() == null) {
            return false;
        }
        minecraft.setScreen(new GuiMap(null, null, session.getMapProcessor(), player));
        return minecraft.screen instanceof GuiMap;
    }

    public static boolean isWorldMapScreen(Screen screen) {
        return screen instanceof GuiMap;
    }

    /** Inject Destiny hub tabs into GuiMap using its public addRenderableWidget. */
    public static void attachHubTabBar(Screen screen) {
        if (!(screen instanceof GuiMap map)) {
            return;
        }
        RpgHubTabBar.addTo(map::addRenderableWidget, screen.width, RpgHubTab.MAP);
    }
}
