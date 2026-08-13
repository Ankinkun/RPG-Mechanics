package com.ankin.rpgmechanics.world.border.client;

import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private static final ResourceLocation FORCEFIELD =
            ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
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

        double depthFar = minecraft.gameRenderer.getDepthFar();
        float scroll = (float) (Util.getMillis() % 3000L) / 3000.0F;
        float vBottom = (float) (-Mth.frac(cam.y * 0.5));
        float vTop = vBottom + (float) depthFar;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderTexture(0, FORCEFIELD);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        // Vanilla-like stationary border blue.
        RenderSystem.setShaderColor(0.25F, 0.70F, 1.0F, 0.85F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.disableCull();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        List<BorderPolygon.Vertex> verts = definition.polygon().vertices();
        int count = verts.size();
        for (int i = 0; i < count; i++) {
            BorderPolygon.Vertex a = verts.get(i);
            BorderPolygon.Vertex b = verts.get((i + 1) % count);
            emitSegment(buffer, cam.x, cam.z, depthFar, a.x(), a.z(), b.x(), b.z(), viewRadius, scroll, vBottom, vTop);
        }

        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableCull();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
    }

    private static void emitSegment(
            BufferBuilder buffer,
            double camX,
            double camZ,
            double depthFar,
            double ax,
            double az,
            double bx,
            double bz,
            double viewRadius,
            float scroll,
            float vBottom,
            float vTop
    ) {
        double dx = bx - ax;
        double dz = bz - az;
        double len = Math.hypot(dx, dz);
        if (len < 1.0E-4) {
            return;
        }
        double ux = dx / len;
        double uz = dz / len;
        int steps = Math.max(1, (int) Math.ceil(len));
        double step = len / steps;
        float uCursor = (float) (Mth.floor(ax) & 1) * 0.5F;
        for (int i = 0; i < steps; i++) {
            double t0 = i * step;
            double t1 = Math.min(len, (i + 1) * step);
            double x0 = ax + ux * t0;
            double z0 = az + uz * t0;
            double x1 = ax + ux * t1;
            double z1 = az + uz * t1;
            if (Math.hypot((x0 + x1) * 0.5 - camX, (z0 + z1) * 0.5 - camZ) > viewRadius) {
                uCursor += (float) ((t1 - t0) * 0.5);
                continue;
            }
            float segU = (float) ((t1 - t0) * 0.5);
            float u0 = scroll + uCursor;
            float u1 = scroll + uCursor + segU;
            buffer.addVertex((float) (x0 - camX), (float) (-depthFar), (float) (z0 - camZ)).setUv(u0, scroll + vTop);
            buffer.addVertex((float) (x1 - camX), (float) (-depthFar), (float) (z1 - camZ)).setUv(u1, scroll + vTop);
            buffer.addVertex((float) (x1 - camX), (float) depthFar, (float) (z1 - camZ)).setUv(u1, scroll + vBottom);
            buffer.addVertex((float) (x0 - camX), (float) depthFar, (float) (z0 - camZ)).setUv(u0, scroll + vBottom);
            uCursor += segU;
        }
    }
}
