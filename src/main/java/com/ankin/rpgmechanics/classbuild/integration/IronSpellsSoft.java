package com.ankin.rpgmechanics.classbuild.integration;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;

/**
 * Soft entry points that do not hard-link Iron Spells classes into always-loaded code.
 */
public final class IronSpellsSoft {
    public static final String MANAGED_TAG = "rpgmechanics_managed_spellbook";

    private IronSpellsSoft() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("irons_spellbooks") && ModList.get().isLoaded("curios");
    }

    public static boolean isManaged(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(MANAGED_TAG);
    }

    public static void markManaged(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(MANAGED_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void apply(ServerPlayer player, ClassBuildState build, int spellLevel) {
        if (!isAvailable()) {
            return;
        }
        invoke("apply", new Class<?>[] {ServerPlayer.class, ClassBuildState.class, int.class}, player, build, spellLevel);
    }

    public static void reconcile(ServerPlayer player, ClassBuildState build, int spellLevel) {
        if (!isAvailable()) {
            return;
        }
        invoke("reconcile", new Class<?>[] {ServerPlayer.class, ClassBuildState.class, int.class}, player, build, spellLevel);
    }

    public static void registerIntegrationEvents() {
        if (!isAvailable()) {
            RpgMechanics.LOGGER.info("Iron Spells/Curios not present — class spellbook bridge disabled");
            return;
        }
        try {
            Class<?> events = Class.forName("com.ankin.rpgmechanics.classbuild.ClassBuildIntegrationEvents");
            events.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.error("Failed to register class-build ISS integration", exception);
        }
    }

    private static void invoke(String method, Class<?>[] types, Object... args) {
        try {
            Class<?> bridge = Class.forName("com.ankin.rpgmechanics.classbuild.integration.IronSpellsBridge");
            bridge.getMethod(method, types).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            RpgMechanics.LOGGER.error("Iron Spells bridge call {} failed", method, exception);
        }
    }
}
