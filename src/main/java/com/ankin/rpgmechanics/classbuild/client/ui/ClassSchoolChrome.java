package com.ankin.rpgmechanics.classbuild.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Procedural Class-hub chrome: clean school seals, slot accents, and a catacomb player panel.
 */
public final class ClassSchoolChrome {
    private ClassSchoolChrome() {
    }

    /** Ability / fragment / popup panels — keep school accents, avoid noisy crack fills. */
    public static void themedPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            ClassSchoolTheme theme,
            float alpha,
            float ticks
    ) {
        themedPanel(graphics, x, y, w, h, theme.panel, theme.panelDark, theme.accent, theme, alpha, ticks);
    }

    public static void themedPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int panel,
            int panelDark,
            int accent,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        int a = (int) (0xD8 * alpha);
        ClassSkillUi.fillRounded(graphics, x, y, w, h, 5, ClassSkillUi.withAlpha(panelDark, a));
        ClassSkillUi.fillRounded(
                graphics,
                x + 2,
                y + 2,
                Math.max(1, w - 4),
                Math.max(1, h - 4),
                3,
                ClassSkillUi.withAlpha(panel, (int) (0x70 * alpha))
        );
        ClassSkillUi.outlineRounded(graphics, x, y, w, h, 5, ClassSkillUi.withAlpha(accent, (int) (0xEE * alpha)));
        ClassSkillUi.outlineRounded(
                graphics,
                x + 1,
                y + 1,
                Math.max(1, w - 2),
                Math.max(1, h - 2),
                4,
                ClassSkillUi.withAlpha(panelDark, (int) (0xAA * alpha))
        );

        switch (school) {
            case BLOOD -> bloodPanelExtras(graphics, x, y, w, h, accent, alpha, ticks);
            case ELDRITCH -> eldritchPanelExtras(graphics, x, y, w, h, accent, alpha, ticks);
            case ENDER -> enderPanelExtras(graphics, x, y, w, h, accent, alpha, ticks);
        }
    }

    /**
     * Player preview: dark fading catacomb brick wall (not crack scribble).
     */
    public static void catacombPlayerPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int panel,
            int panelDark,
            int accent,
            float alpha
    ) {
        ClassSkillUi.fillRounded(graphics, x, y, w, h, 6, ClassSkillUi.withAlpha(panelDark, (int) (0xF0 * alpha)));
        drawCatacombBricks(graphics, x + 3, y + 3, w - 6, h - 6, panel, panelDark, accent, alpha);
        // Soft vignette — fades to black at edges
        vignetteRect(graphics, x + 2, y + 2, w - 4, h - 4, panelDark, alpha);
        ClassSkillUi.outlineRounded(graphics, x, y, w, h, 6, ClassSkillUi.withAlpha(accent, (int) (0xDD * alpha)));
        ClassSkillUi.outlineRounded(
                graphics,
                x + 1,
                y + 1,
                w - 2,
                h - 2,
                5,
                ClassSkillUi.withAlpha(panelDark, (int) (0xBB * alpha))
        );
    }

    public static void themedSlot(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            int panel,
            int panelDark,
            int accent,
            int focus,
            boolean hot,
            ClassSchoolTheme school,
            float ticks
    ) {
        int border = hot ? focus : accent;
        ClassSkillUi.fillRounded(graphics, x, y, size, size, 3, ClassSkillUi.withAlpha(panelDark, 0xE8));
        ClassSkillUi.fillRounded(
                graphics,
                x + 2,
                y + 2,
                size - 4,
                size - 4,
                2,
                ClassSkillUi.withAlpha(panel, 0x55)
        );
        ClassSkillUi.outlineRounded(graphics, x, y, size, size, 3, border);
        ClassSkillUi.outlineRounded(
                graphics,
                x + 1,
                y + 1,
                size - 2,
                size - 2,
                2,
                ClassSkillUi.withAlpha(panelDark, 0xCC)
        );

        switch (school) {
            case BLOOD -> {
                graphics.fill(x + 2, y + size - 4, x + 5, y + size - 1, ClassSkillUi.withAlpha(accent, 0xAA));
                if (hot) {
                    drip(graphics, x + size / 2, y + size - 1, 4, accent, ticks);
                }
            }
            case ELDRITCH -> {
                int ex = x + size - 6;
                int ey = y + 3;
                graphics.fill(ex, ey, ex + 3, ey + 3, ClassSkillUi.withAlpha(accent, 0xCC));
                graphics.fill(ex + 1, ey + 1, ex + 2, ey + 2, 0xFF050808);
            }
            case ENDER -> {
                mote(graphics, x + 3, y + 3, accent, ticks, 0.7f);
                mote(graphics, x + size - 4, y + size - 5, accent, ticks + 11f, 0.55f);
            }
        }
    }

    /**
     * Clean ritual disc behind the hero book.
     * Blood = sealed crimson well; Eldritch = arcane iris; Ender = void motes.
     */
    public static void schoolRingBackdrop(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        if (alpha < 0.05f) {
            return;
        }
        // Few soft discs — not a per-pixel concentric loop
        ClassSkillUi.fillCircle(
                graphics,
                cx,
                cy,
                radius,
                ClassSkillUi.withAlpha(school.panelDark, (int) (0xE8 * alpha))
        );
        ClassSkillUi.fillCircle(
                graphics,
                cx,
                cy,
                Math.max(2, radius * 2 / 3),
                ClassSkillUi.withAlpha(school.panel, (int) (0x66 * alpha))
        );
        ClassSkillUi.fillCircle(
                graphics,
                cx,
                cy,
                Math.max(2, radius / 3),
                ClassSkillUi.withAlpha(school.panelDark, (int) (0x88 * alpha))
        );

        ClassSkillUi.ring(graphics, cx, cy, radius, 3, ClassSkillUi.withAlpha(school.accent, (int) (0xFF * alpha)));
        ClassSkillUi.ring(graphics, cx, cy, radius - 5, 1, ClassSkillUi.withAlpha(school.accentDark, (int) (0xAA * alpha)));

        // Sparse rim ticks (8, not 16)
        for (int i = 0; i < 8; i++) {
            float ang = (float) (i * (Math.PI * 2.0 / 8));
            float cos = Mth.cos(ang);
            float sin = Mth.sin(ang);
            int x0 = cx + Math.round(sin * (radius - 2));
            int y0 = cy + Math.round(cos * (radius - 2));
            int len = i % 2 == 0 ? 8 : 5;
            int x1 = cx + Math.round(sin * (radius - len));
            int y1 = cy + Math.round(cos * (radius - len));
            graphics.fill(
                    Math.min(x0, x1),
                    Math.min(y0, y1),
                    Math.max(x0, x1) + 1,
                    Math.max(y0, y1) + 1,
                    ClassSkillUi.withAlpha(school.accent, (int) (0x77 * alpha))
            );
        }

        switch (school) {
            case BLOOD -> bloodRingClean(graphics, cx, cy, radius, school, alpha, ticks);
            case ELDRITCH -> eldritchRingClean(graphics, cx, cy, radius, school, alpha, ticks);
            case ENDER -> enderRingClean(graphics, cx, cy, radius, school, alpha, ticks);
        }
    }

    /** Cheap seal for fan nodes — fill + ring only. */
    public static void schoolRingBackdropLite(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha
    ) {
        if (alpha < 0.05f) {
            return;
        }
        ClassSkillUi.fillCircle(
                graphics,
                cx,
                cy,
                radius,
                ClassSkillUi.withAlpha(school.panelDark, (int) (0xE0 * alpha))
        );
        ClassSkillUi.ring(graphics, cx, cy, radius, 2, ClassSkillUi.withAlpha(school.accent, (int) (0xFF * alpha)));
    }

    /** @deprecated use {@link #schoolRingBackdrop} */
    public static void schoolRingExtras(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        schoolRingBackdrop(graphics, cx, cy, radius, school, alpha, ticks);
    }

    private static void bloodRingClean(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        // Deep pool near bottom of disc — not scribble across the face
        int poolA = (int) (0x66 * alpha);
        ClassSkillUi.fillCircle(
                graphics,
                cx,
                cy + radius / 5,
                radius / 2,
                ClassSkillUi.withAlpha(school.accent, poolA / 2)
        );
        // Controlled drips from bottom rim only
        drip(graphics, cx - radius / 3, cy + radius - 1, 6, school.accent, ticks);
        drip(graphics, cx, cy + radius - 1, 8, school.accent, ticks + 4f);
        drip(graphics, cx + radius / 3, cy + radius - 1, 5, school.accent, ticks + 8f);
        // Soft inner halo
        ClassSkillUi.ring(
                graphics,
                cx,
                cy,
                radius / 2,
                1,
                ClassSkillUi.withAlpha(school.accent, (int) (0x44 * alpha * (0.7f + 0.3f * Mth.sin(ticks * 0.08f))))
        );
    }

    private static void eldritchRingClean(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        // Concentric iris rings — clean arcane seal
        ClassSkillUi.ring(graphics, cx, cy, radius * 2 / 3, 1, ClassSkillUi.withAlpha(school.accent, (int) (0x66 * alpha)));
        ClassSkillUi.ring(graphics, cx, cy, radius / 3, 1, ClassSkillUi.withAlpha(school.accent, (int) (0x55 * alpha)));
        // Single watching iris at center (small, sharp)
        float pulse = 0.75f + 0.25f * Mth.sin(ticks * 0.12f);
        int irisR = Math.max(3, Math.round(5 * pulse));
        ClassSkillUi.fillCircle(graphics, cx, cy, irisR + 2, ClassSkillUi.withAlpha(school.accent, (int) (0x55 * alpha)));
        ClassSkillUi.fillCircle(graphics, cx, cy, irisR, ClassSkillUi.withAlpha(school.panel, (int) (0xAA * alpha)));
        ClassSkillUi.fillCircle(graphics, cx, cy, Math.max(1, irisR / 2), ClassSkillUi.withAlpha(0xFF020808, (int) (0xF0 * alpha)));
        // Four subtle tendril tips at cardinal points only
        for (int i = 0; i < 4; i++) {
            float ang = i * ((float) Math.PI * 0.5f) + ticks * 0.02f;
            int x0 = cx + Math.round(Mth.sin(ang) * (radius - 10));
            int y0 = cy + Math.round(Mth.cos(ang) * (radius - 10));
            int x1 = cx + Math.round(Mth.sin(ang) * (radius - 3));
            int y1 = cy + Math.round(Mth.cos(ang) * (radius - 3));
            tendril(graphics, x0, y0, x1, y1, school.accent, alpha * 0.7f);
        }
    }

    private static void enderRingClean(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            ClassSchoolTheme school,
            float alpha,
            float ticks
    ) {
        for (int i = 0; i < 5; i++) {
            float ang = ticks * 0.08f + i * 1.25f;
            float r = radius * (0.4f + 0.4f * Mth.sin(ticks * 0.14f + i));
            int mx = cx + Math.round(Mth.sin(ang) * r);
            int my = cy + Math.round(Mth.cos(ang) * r);
            mote(graphics, mx, my, school.accent, ticks + i * 3f, alpha);
        }
    }

    private static void drawCatacombBricks(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int panel,
            int panelDark,
            int accent,
            float alpha
    ) {
        final int brickH = 14;
        final int brickW = 24;
        final int mortar = ClassSkillUi.withAlpha(0xFF050308, (int) (0xEE * alpha));
        int row = 0;
        for (int by = y; by < y + h; by += brickH) {
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int bx = x - offset; bx < x + w; bx += brickW) {
                int x0 = Math.max(x, bx);
                int y0 = by;
                int x1 = Math.min(x + w, bx + brickW - 1);
                int y1 = Math.min(y + h, by + brickH - 1);
                if (x1 <= x0 || y1 <= y0) {
                    continue;
                }
                int shade = ((bx * 13 + by * 7) & 3);
                int face = shade == 0 ? panelDark : (shade == 1 ? panel : lerpColor(panelDark, panel, 0.45f));
                graphics.fill(x0, y0, x1, y1, ClassSkillUi.withAlpha(face, (int) (0xE0 * alpha)));
                graphics.fill(x0, y1 - 1, x1, y1, mortar);
                graphics.fill(x1 - 1, y0, x1, y1, mortar);
            }
            row++;
        }
    }

    private static void vignetteRect(GuiGraphics graphics, int x, int y, int w, int h, int dark, float alpha) {
        // 3 edge bands + floor shadow (was 8 full perimeter passes)
        int a1 = (int) (0x28 * alpha);
        int a2 = (int) (0x44 * alpha);
        int a3 = (int) (0x66 * alpha);
        graphics.fill(x, y, x + w, y + 6, ClassSkillUi.withAlpha(dark, a1));
        graphics.fill(x, y, x + 6, y + h, ClassSkillUi.withAlpha(dark, a1));
        graphics.fill(x + w - 6, y, x + w, y + h, ClassSkillUi.withAlpha(dark, a1));
        graphics.fill(x, y, x + w, y + 3, ClassSkillUi.withAlpha(dark, a2));
        graphics.fill(x, y, x + 3, y + h, ClassSkillUi.withAlpha(dark, a2));
        graphics.fill(x + w - 3, y, x + w, y + h, ClassSkillUi.withAlpha(dark, a2));
        graphics.fill(x, y + h * 2 / 3, x + w, y + h, ClassSkillUi.withAlpha(0xFF000000, a3));
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        return (Mth.lerpInt(t, aa, ba) << 24)
                | (Mth.lerpInt(t, ar, br) << 16)
                | (Mth.lerpInt(t, ag, bg) << 8)
                | Mth.lerpInt(t, ab, bb);
    }

    private static void bloodPanelExtras(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int accent,
            float alpha,
            float ticks
    ) {
        int clot = ClassSkillUi.withAlpha(accent, (int) (0x88 * alpha));
        graphics.fill(x + 2, y + 2, x + 6, y + 4, clot);
        graphics.fill(x + w - 7, y + h - 3, x + w - 2, y + h - 1, clot);
        drip(graphics, x + 10, y + h - 1, 4, accent, ticks);
        drip(graphics, x + w - 12, y + h - 1, 5, accent, ticks + 6f);
    }

    private static void eldritchPanelExtras(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int accent,
            float alpha,
            float ticks
    ) {
        eye(graphics, x + 5, y + 4, accent, alpha * 0.85f, ticks);
        eye(graphics, x + w - 10, y + 4, accent, alpha * 0.85f, ticks + 3f);
    }

    private static void enderPanelExtras(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            int h,
            int accent,
            float alpha,
            float ticks
    ) {
        for (int i = 0; i < 5; i++) {
            int mx = x + 4 + ((i * 29 + (int) (ticks * 1.5f)) % Math.max(1, w - 8));
            int my = y + 4 + ((i * 17 + (int) (ticks * 0.8f)) % Math.max(1, h - 8));
            mote(graphics, mx, my, accent, ticks + i * 5f, alpha);
        }
        int shard = ClassSkillUi.withAlpha(accent, (int) (0x88 * alpha));
        graphics.fill(x + 1, y + 1, x + 4, y + 2, shard);
        graphics.fill(x + 1, y + 1, x + 2, y + 5, shard);
        graphics.fill(x + w - 4, y + h - 2, x + w - 1, y + h - 1, shard);
        graphics.fill(x + w - 2, y + h - 5, x + w - 1, y + h - 1, shard);
    }

    private static void drip(GuiGraphics graphics, int x, int y, int length, int accent, float ticks) {
        int len = Math.max(2, length + Math.round(Mth.sin(ticks * 0.25f) * 1.5f));
        graphics.fill(x, y, x + 1, y + len, ClassSkillUi.withAlpha(accent, 0xBB));
        graphics.fill(x - 1, y + len, x + 2, y + len + 2, ClassSkillUi.withAlpha(accent, 0x99));
    }

    private static void tendril(GuiGraphics graphics, int x0, int y0, int x1, int y1, int accent, float alpha) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        steps = Math.max(1, steps);
        int px = x0;
        int py = y0;
        int color = ClassSkillUi.withAlpha(accent, (int) (0xAA * alpha));
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            int nx = Math.round(Mth.lerp(t, x0, x1));
            int ny = Math.round(Mth.lerp(t, y0, y1));
            graphics.fill(Math.min(px, nx), Math.min(py, ny), Math.max(px, nx) + 1, Math.max(py, ny) + 1, color);
            px = nx;
            py = ny;
        }
        graphics.fill(x1, y1, x1 + 2, y1 + 2, ClassSkillUi.withAlpha(accent, (int) (0xCC * alpha)));
    }

    private static void eye(GuiGraphics graphics, int x, int y, int accent, float alpha, float ticks) {
        int open = 2 + Math.round(0.5f * Mth.sin(ticks * 0.18f));
        graphics.fill(x, y, x + 5, y + open + 1, ClassSkillUi.withAlpha(accent, (int) (0xAA * alpha)));
        graphics.fill(x + 2, y + open / 2, x + 3, y + open / 2 + 1, ClassSkillUi.withAlpha(0xFF030606, (int) (0xFF * alpha)));
    }

    private static void mote(GuiGraphics graphics, int x, int y, int accent, float ticks, float alpha) {
        float pulse = 0.55f + 0.45f * Mth.sin(ticks * 0.35f);
        int a = (int) (0xFF * alpha * pulse);
        graphics.fill(x, y, x + 1, y + 1, ClassSkillUi.withAlpha(accent, a));
        graphics.fill(x - 1, y, x, y + 1, ClassSkillUi.withAlpha(0xFF1A0A28, (int) (a * 0.6f)));
    }
}
