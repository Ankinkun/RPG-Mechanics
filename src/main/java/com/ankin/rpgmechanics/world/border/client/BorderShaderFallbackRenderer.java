package com.ankin.rpgmechanics.world.border.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Optional soft forcefield wall when a shader pack is active.
 * <p>
 * Default <strong>off</strong> — with the Photon {@code fogEnd} patch, distance-driven fog is enough.
 * Re-enable via CLIENT {@code world.borderShaderFallbackWall}. Manual debug wall is separate.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class BorderShaderFallbackRenderer {
    private static final double VIEW_RADIUS = 160.0;

    private BorderShaderFallbackRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!fallbackEnabled()) {
            return;
        }
        if (!ShaderPackCompat.isShaderPackInUse()) {
            return;
        }
        if (BorderDebugWallRenderer.isVisible()) {
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
        double edgeDist = definition.polygon().distanceToEdge(cam.x, cam.z);
        if (edgeDist > viewRadius) {
            return;
        }

        float soft = (float) Math.max(32.0, definition.fogDepth() * 1.5);
        float proximity = 1.0F - (float) Mth.clamp(edgeDist / soft, 0.0, 1.0);
        proximity = proximity * proximity * (3.0F - 2.0F * proximity);
        if (definition.polygon().distanceOutside(cam.x, cam.z) > 0.0) {
            proximity = Math.max(proximity, 0.85F);
        }
        if (proximity < 0.05F) {
            return;
        }

        float alpha = 0.12F + proximity * 0.35F;
        BorderWallPainter.draw(
                definition.polygon(),
                cam.x,
                cam.z,
                viewRadius,
                0.35F,
                0.75F,
                1.0F,
                alpha
        );
    }

    private static boolean fallbackEnabled() {
        if (!RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            return false;
        }
        try {
            return RpgMechanicsConfig.CLIENT.borderShaderFallbackWall.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
