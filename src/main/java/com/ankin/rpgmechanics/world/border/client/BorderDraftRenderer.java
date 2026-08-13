package com.ankin.rpgmechanics.world.border.client;

import java.util.List;
import java.util.Optional;

import org.joml.Matrix4f;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Admin preview of the in-progress border draft: vertex poles + connecting edges.
 * Visible only to permission 2+ clients with a synced draft.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class BorderDraftRenderer {
    private static final float POLE_DOWN = 12.0F;
    private static final float POLE_UP = 28.0F;

    private BorderDraftRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!minecraft.player.hasPermissions(2)) {
            return;
        }
        BorderPolygon draft = ClientWorldBorderCache.draft(minecraft.level.dimension().location());
        if (draft == null || draft.vertices().isEmpty()) {
            return;
        }

        List<BorderPolygon.Vertex> verts = draft.vertices();
        Optional<String> problem = draft.validationMessage();
        boolean bad = problem.isPresent() && verts.size() >= 3;
        float er = bad ? 1.0F : 1.0F;
        float eg = bad ? 0.25F : 0.75F;
        float eb = bad ? 0.2F : 0.15F;
        float ea = 0.95F;
        float ghostA = 0.35F;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        float eyeY = (float) cam.y;
        // Connected edges (open polyline).
        for (int i = 0; i < verts.size() - 1; i++) {
            BorderPolygon.Vertex a = verts.get(i);
            BorderPolygon.Vertex b = verts.get(i + 1);
            line(lines, matrix, a.x(), eyeY, a.z(), b.x(), eyeY, b.z(), er, eg, eb, ea);
            // Second line slightly lower for thickness readability.
            line(lines, matrix, a.x(), eyeY - 0.15F, a.z(), b.x(), eyeY - 0.15F, b.z(), er, eg, eb, ea * 0.7F);
        }
        // Ghost close edge once we have 3+ verts.
        if (verts.size() >= 3) {
            BorderPolygon.Vertex first = verts.get(0);
            BorderPolygon.Vertex last = verts.get(verts.size() - 1);
            line(lines, matrix, last.x(), eyeY, last.z(), first.x(), eyeY, first.z(), er, eg, eb, ghostA);
        }

        // Vertex poles.
        for (int i = 0; i < verts.size(); i++) {
            BorderPolygon.Vertex v = verts.get(i);
            boolean last = i == verts.size() - 1;
            float pr = last ? 0.2F : er;
            float pg = last ? 1.0F : eg;
            float pb = last ? 0.35F : eb;
            line(lines, matrix, v.x(), eyeY - POLE_DOWN, v.z(), v.x(), eyeY + POLE_UP, v.z(), pr, pg, pb, 1.0F);
            // Small cross at eye height.
            line(lines, matrix, v.x() - 0.6F, eyeY, v.z(), v.x() + 0.6F, eyeY, v.z(), pr, pg, pb, 1.0F);
            line(lines, matrix, v.x(), eyeY, v.z() - 0.6F, v.x(), eyeY, v.z() + 0.6F, pr, pg, pb, 1.0F);
        }

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void line(
            VertexConsumer consumer,
            Matrix4f matrix,
            double x0, float y0, double z0,
            double x1, float y1, double z1,
            float r, float g, float b, float a
    ) {
        float dx = (float) (x1 - x0);
        float dy = y1 - y0;
        float dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 1.0E-4F ? dx / len : 0.0F;
        float ny = len > 1.0E-4F ? dy / len : 1.0F;
        float nz = len > 1.0E-4F ? dz / len : 0.0F;
        consumer.addVertex(matrix, (float) x0, y0, (float) z0)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
        consumer.addVertex(matrix, (float) x1, y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz);
    }
}
