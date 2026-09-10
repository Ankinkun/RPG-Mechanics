package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Pins hotbar to slot 0 and blocks wheel scrolling while combat HUD / active character.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class CombatHotbarGuard {
    private CombatHotbarGuard() {
    }

    private static boolean active() {
        if (!ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isCreative()) {
            return false;
        }
        if (!RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.CLIENT.classbuildCombatHud.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (player.getInventory().selected != 0) {
            player.getInventory().selected = 0;
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        event.setCanceled(true);
    }
}
