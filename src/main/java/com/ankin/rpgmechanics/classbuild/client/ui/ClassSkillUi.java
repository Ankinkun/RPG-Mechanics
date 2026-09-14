package com.ankin.rpgmechanics.classbuild.client.ui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

/**
 * Drawing helpers for the Class skill hub (rings, glow, eased fills, 3D items).
 */
public final class ClassSkillUi {
    private ClassSkillUi() {
    }

    public static float easeOutCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float easeOutBack(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    public static float approach(float current, float target, float speed) {
        return current + (target - current) * Mth.clamp(speed, 0f, 1f);
    }

    public static int withAlpha(int argb, int alpha) {
        int a = Mth.clamp(alpha, 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Stable fan-out point on a horizontal arc below an origin.
     * {@code index} in {@code [0, count)}, {@code progress} 0..1.
     */
    public static Vec2 fanArc(float originX, float originY, int index, int count, float progress, float radius) {
        float t = easeOutBack(Mth.clamp(progress, 0f, 1f));
        float span = 100f;
        float start = -span * 0.5f;
        float step = count <= 1 ? 0f : span / (count - 1);
        float deg = start + step * index;
        float rad = (float) Math.toRadians(deg);
        float r = radius * t;
        float x = originX + Mth.sin(rad) * r;
        float y = originY + 12f * t + (1f - Mth.cos(rad)) * r * 0.35f;
        return new Vec2(x, y);
    }

    /**
     * Upright spinning Iron's spellbook centered on (cx, cy).
     * {@link ItemRenderer} already applies {@code translate(-0.5)} — do not repeat it.
     * Pass {@code flush=false} when drawing several books, then {@link GuiGraphics#flush()} once.
     */
    public static void renderSpinningItem(
            GuiGraphics graphics,
            ItemStack stack,
            float cx,
            float cy,
            float scale,
            float ticks,
            float spinOffset
    ) {
        renderSpinningItem(graphics, stack, cx, cy, scale, ticks, spinOffset, true);
    }

    public static void renderSpinningItem(
            GuiGraphics graphics,
            ItemStack stack,
            float cx,
            float cy,
            float scale,
            float ticks,
            float spinOffset,
            boolean flush
    ) {
        if (stack == null || stack.isEmpty() || scale < 0.5f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        float poseScale = scale * (16f / 7f);
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);

        float bob = Mth.sin((ticks + spinOffset) * 0.1f) * 1.2f;
        float yaw = (ticks + spinOffset) * 2.8f;
        float tip = 20f;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy + bob, 200);
        graphics.pose().scale(poseScale, -poseScale, poseScale);
        graphics.pose().mulPose(Axis.XP.rotationDegrees(tip));
        graphics.pose().mulPose(Axis.YP.rotationDegrees(yaw));
        graphics.pose().translate(0f, 0.5f - 3.5f / 16f, 0f);

        Lighting.setupFor3DItems();
        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                graphics.pose(),
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model
        );
        if (flush) {
            graphics.flush();
            Lighting.setupForFlatItems();
        }
        graphics.pose().popPose();
    }

    public static void fillRounded(GuiGraphics graphics, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        graphics.fill(x + r, y, x + w - r, y + h, color);
        graphics.fill(x, y + r, x + w, y + h - r, color);
        fillCircle(graphics, x + r, y + r, r, color);
        fillCircle(graphics, x + w - r - 1, y + r, r, color);
        fillCircle(graphics, x + r, y + h - r - 1, r, color);
        fillCircle(graphics, x + w - r - 1, y + h - r - 1, r, color);
    }

    public static void outlineRounded(GuiGraphics graphics, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        graphics.fill(x + r, y, x + w - r, y + 1, color);
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
        graphics.fill(x, y + r, x + 1, y + h - r, color);
        graphics.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i <= r; i++) {
            int dy = (int) Math.round(Math.sqrt(r * r - i * i));
            graphics.fill(x + r - i, y + r - dy, x + r - i + 1, y + r - dy + 1, color);
            graphics.fill(x + w - r + i - 1, y + r - dy, x + w - r + i, y + r - dy + 1, color);
            pixel(graphics, x + r - i, y + h - r + dy - 1, color);
            pixel(graphics, x + w - r + i - 1, y + h - r + dy - 1, color);
        }
    }

    private static void pixel(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    public static void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.floor(Math.sqrt(radius * radius - dy * dy));
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    public static void ring(GuiGraphics graphics, int cx, int cy, int outerR, int thickness, int color) {
        int inner = Math.max(0, outerR - thickness);
        for (int dy = -outerR; dy <= outerR; dy++) {
            int outerDx = (int) Math.floor(Math.sqrt(outerR * outerR - dy * dy));
            int innerDx = dy * dy > inner * inner ? 0 : (int) Math.floor(Math.sqrt(inner * inner - dy * dy));
            if (outerDx > innerDx) {
                graphics.fill(cx - outerDx, cy + dy, cx - innerDx, cy + dy + 1, color);
                graphics.fill(cx + innerDx + 1, cy + dy, cx + outerDx + 1, cy + dy + 1, color);
            }
        }
    }

    /** Soft glow with a few layers (avoid dozens of full circle fills). */
    public static void glowDisc(GuiGraphics graphics, int cx, int cy, int radius, int glowArgb) {
        int baseA = (glowArgb >>> 24) & 0xFF;
        fillCircle(graphics, cx, cy, radius, withAlpha(glowArgb, (int) (baseA * 0.18f)));
        fillCircle(graphics, cx, cy, Math.max(1, radius * 2 / 3), withAlpha(glowArgb, (int) (baseA * 0.28f)));
        fillCircle(graphics, cx, cy, Math.max(1, radius / 3), withAlpha(glowArgb, (int) (baseA * 0.4f)));
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h, int panelColor, int accentColor, float alpha) {
        int a = (int) (0xCC * alpha);
        fillRounded(graphics, x, y, w, h, 6, withAlpha(panelColor, a));
        outlineRounded(graphics, x, y, w, h, 6, withAlpha(accentColor, (int) (0xFF * alpha)));
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h, ClassSchoolTheme theme, float alpha) {
        panel(graphics, x, y, w, h, theme.panel, theme.accent, alpha);
    }
}
