package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.RpgMechanics;

import net.neoforged.fml.ModList;

/**
 * Soft client Epic Fight helpers (skill UI open).
 */
public final class EpicFightClientSoft {
    private EpicFightClientSoft() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("epicfight");
    }

    /** @return true if the skill edit screen was opened */
    public static boolean openSkillEditScreen() {
        if (!isAvailable()) {
            return false;
        }
        Object value = invoke("openSkillEditScreen", new Class<?>[] {});
        return value instanceof Boolean b && b;
    }

    public static void ensureCombatMode() {
        if (!isAvailable()) {
            return;
        }
        invoke("ensureCombatMode", new Class<?>[] {});
    }

    private static Object invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> bridge = Class.forName("com.ankin.rpgmechanics.classbuild.integration.EpicFightClientBridge");
            return bridge.getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.debug("Epic Fight client bridge {} failed: {}", method, exception.toString());
            return null;
        }
    }
}
