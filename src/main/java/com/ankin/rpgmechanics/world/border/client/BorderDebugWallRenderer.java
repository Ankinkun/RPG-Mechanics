package com.ankin.rpgmechanics.world.border.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Debug-only light-blue striped wall on the polygon edge (vanilla forcefield look).
 * Toggle: {@code /rpgmechanics world border debugwall}
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class BorderDebugWallRenderer {
    private static final double VIEW_RADIUS = 160.0;

    private static boolean visible = false;

    private BorderDebugWallRenderer() {
    }

    public static boolean isVisible() {
        return visible;
    }

    public static boolean toggle() {
        visible = !visible;
        return visible;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!visible || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!ClientWorldBorderCache.isFeatureEnabled()) {
            return;
        }
        WorldBorderDefinition definition = ClientWorldBorderCache.get(minecraft.level.dimension().location());
        if (definition == null || !definition.enabled() || !definition.polygon().isValid()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        double viewRadius = Math.min(VIEW_RADIUS, minecraft.options.getEffectiveRenderDistance() * 16.0);
        if (definition.polygon().distanceToEdge(cam.x, cam.z) > viewRadius) {
            return;
        }

        BorderWallPainter.draw(
                definition.polygon(),
                cam.x,
                cam.z,
                viewRadius,
                0.25F,
                0.70F,
                1.0F,
                0.85F
        );
    }
}
