package com.ankin.rpgmechanics.world.border.client;

import com.ankin.rpgmechanics.RpgMechanics;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Experimental per-pixel world-space border fog (depth unprojection).
 * <p>
 * Currently <strong>disabled</strong> — reconstruction produced water-plane / sky artifacts
 * on 1.21.1. Stable visuals live in {@link BorderFogRenderer}. Shader assets remain under
 * {@code assets/rpgmechanics/shaders/core/} for a future attempt.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class BorderWorldFogRenderer {
    private BorderWorldFogRenderer() {
    }

    public static void registerShaders(RegisterShadersEvent event) {
        // Shader registration skipped while the pass is disabled.
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // no-op
    }
}
