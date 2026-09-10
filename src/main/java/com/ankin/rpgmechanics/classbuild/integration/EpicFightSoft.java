package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Soft Epic Fight entry points (no hard link from always-loaded server code).
 */
public final class EpicFightSoft {
    private EpicFightSoft() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("epicfight");
    }

    public static void grantStarterSkills(ServerPlayer player) {
        if (!isAvailable() || player == null) {
            return;
        }
        invoke("grantStarterSkills", new Class<?>[] {ServerPlayer.class}, player);
    }

    /** Force Epic Fight combat mode (not mining/vanilla mode). */
    public static void ensureCombatMode(ServerPlayer player) {
        if (!isAvailable() || player == null) {
            return;
        }
        invoke("ensureCombatMode", new Class<?>[] {ServerPlayer.class}, player);
    }

    public static boolean isTwoHanded(ServerPlayer player, ItemStack stack) {
        if (!isAvailable() || player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Object value = invoke("isTwoHanded", new Class<?>[] {ServerPlayer.class, ItemStack.class}, player, stack);
        return value instanceof Boolean b && b;
    }

    private static Object invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> bridge = Class.forName("com.ankin.rpgmechanics.classbuild.integration.EpicFightBridge");
            return bridge.getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.debug("Epic Fight bridge {} failed: {}", method, exception.toString());
            return null;
        }
    }
}
