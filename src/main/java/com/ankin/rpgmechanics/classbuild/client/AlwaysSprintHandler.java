package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class AlwaysSprintHandler {
    private AlwaysSprintHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null
                && minecraft.screen == null
                && !player.isPassenger()
                && player.input.forwardImpulse > 0.0F
                && !player.isShiftKeyDown()) {
            player.setSprinting(true);
        }
    }

    private static boolean enabled() {
        if (!RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.CLIENT.alwaysSprintWhenMoving.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }
}
