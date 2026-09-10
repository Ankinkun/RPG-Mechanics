package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Soft client-side ISS reads (mana, cooldowns, icons) without hard-loading ISS on the dedicated server.
 */
public final class IronSpellsClientSoft {
    private IronSpellsClientSoft() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("irons_spellbooks");
    }

    public static int getMana() {
        if (!isAvailable()) {
            return 0;
        }
        Object value = invoke("getMana", new Class<?>[] {});
        return value instanceof Integer i ? i : 0;
    }

    public static int getMaxMana(Player player) {
        if (!isAvailable() || player == null) {
            return 100;
        }
        Object value = invoke("getMaxMana", new Class<?>[] {Player.class}, player);
        return value instanceof Integer i ? Math.max(1, i) : 100;
    }

    public static float getCooldownPercent(ResourceLocation spellId) {
        if (!isAvailable() || spellId == null) {
            return 0f;
        }
        Object value = invoke("getCooldownPercent", new Class<?>[] {ResourceLocation.class}, spellId);
        return value instanceof Float f ? f : 0f;
    }

    public static ResourceLocation getSpellIcon(ResourceLocation spellId) {
        if (!isAvailable() || spellId == null) {
            return null;
        }
        Object value = invoke("getSpellIcon", new Class<?>[] {ResourceLocation.class}, spellId);
        return value instanceof ResourceLocation rl ? rl : null;
    }

    private static Object invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> bridge = Class.forName("com.ankin.rpgmechanics.classbuild.integration.IronSpellsClientBridge");
            return bridge.getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.debug("Iron Spells client bridge {} failed: {}", method, exception.toString());
            return null;
        }
    }
}
