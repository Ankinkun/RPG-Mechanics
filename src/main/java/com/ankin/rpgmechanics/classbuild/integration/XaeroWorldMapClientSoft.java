package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

/**
 * Soft client Xaero's World Map helpers (open map + screen detect).
 */
public final class XaeroWorldMapClientSoft {
    private XaeroWorldMapClientSoft() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("xaeroworldmap");
    }

    public static boolean openWorldMap() {
        if (!isAvailable()) {
            return false;
        }
        Object value = invoke("openWorldMap", new Class<?>[] {});
        return value instanceof Boolean b && b;
    }

    public static boolean isWorldMapScreen(Screen screen) {
        if (!isAvailable() || screen == null) {
            return false;
        }
        Object value = invoke("isWorldMapScreen", new Class<?>[] {Screen.class}, screen);
        return value instanceof Boolean b && b;
    }

    public static void attachHubTabBar(Screen screen) {
        if (!isAvailable() || screen == null) {
            return;
        }
        invoke("attachHubTabBar", new Class<?>[] {Screen.class}, screen);
    }

    private static Object invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> bridge = Class.forName("com.ankin.rpgmechanics.classbuild.integration.XaeroWorldMapClientBridge");
            return bridge.getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.debug("Xaero World Map bridge {} failed: {}", method, exception.toString());
            return null;
        }
    }
}
