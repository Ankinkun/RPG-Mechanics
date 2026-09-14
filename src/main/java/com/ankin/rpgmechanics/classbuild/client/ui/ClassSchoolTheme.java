package com.ankin.rpgmechanics.classbuild.client.ui;

import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Ultimate/spellbook school chrome for the Class hub tab.
 */
public enum ClassSchoolTheme {
    BLOOD(0xFF4A0A10, 0xFF120206, 0xFFC43848, 0xFF3A0C14, 0xAAFF2244, 0x55080108),
    ENDER(0xFF2A1040, 0xFF0A0412, 0xFF9B6BFF, 0xFF1E1030, 0xAA9B6BFF, 0x55080618),
    ELDRITCH(0xFF0A3838, 0xFF021212, 0xFF3ECFC9, 0xFF082828, 0xAA3ECFC9, 0x5503080A);

    public final int panel;
    public final int panelDark;
    public final int accent;
    /** Focus / hover — darker than accent, never white. */
    public final int accentDark;
    public final int glow;
    public final int wash;

    ClassSchoolTheme(int panel, int panelDark, int accent, int accentDark, int glow, int wash) {
        this.panel = panel;
        this.panelDark = panelDark;
        this.accent = accent;
        this.accentDark = accentDark;
        this.glow = glow;
        this.wash = wash;
    }

    public static ClassSchoolTheme forUltimate(ResourceLocation ultimate) {
        ResourceLocation book = ClassBuildCatalog.bookForUltimate(ultimate);
        String path = book.getPath();
        if (path.contains("cursed_doll") || path.contains("blood")) {
            return BLOOD;
        }
        if (path.contains("dragonskin") || path.contains("ender")) {
            return ENDER;
        }
        return ELDRITCH;
    }

    /** Mutable ARGB channels for smooth theme crossfades. */
    public static final class Fade {
        public float panelR, panelG, panelB, panelA;
        public float darkR, darkG, darkB, darkA;
        public float accentR, accentG, accentB, accentA;
        public float focusR, focusG, focusB, focusA;
        public float glowR, glowG, glowB, glowA;
        public float washR, washG, washB, washA;

        public Fade(ClassSchoolTheme theme) {
            snapTo(theme);
        }

        public void snapTo(ClassSchoolTheme theme) {
            unpack(theme.panel, true);
            unpackDark(theme.panelDark);
            unpackAccent(theme.accent);
            unpackFocus(theme.accentDark);
            unpackGlow(theme.glow);
            unpackWash(theme.wash);
        }

        public void approach(ClassSchoolTheme target, float speed) {
            float[] p = channels(target.panel);
            panelA = ClassSkillUi.approach(panelA, p[0], speed);
            panelR = ClassSkillUi.approach(panelR, p[1], speed);
            panelG = ClassSkillUi.approach(panelG, p[2], speed);
            panelB = ClassSkillUi.approach(panelB, p[3], speed);
            float[] d = channels(target.panelDark);
            darkA = ClassSkillUi.approach(darkA, d[0], speed);
            darkR = ClassSkillUi.approach(darkR, d[1], speed);
            darkG = ClassSkillUi.approach(darkG, d[2], speed);
            darkB = ClassSkillUi.approach(darkB, d[3], speed);
            float[] a = channels(target.accent);
            accentA = ClassSkillUi.approach(accentA, a[0], speed);
            accentR = ClassSkillUi.approach(accentR, a[1], speed);
            accentG = ClassSkillUi.approach(accentG, a[2], speed);
            accentB = ClassSkillUi.approach(accentB, a[3], speed);
            float[] f = channels(target.accentDark);
            focusA = ClassSkillUi.approach(focusA, f[0], speed);
            focusR = ClassSkillUi.approach(focusR, f[1], speed);
            focusG = ClassSkillUi.approach(focusG, f[2], speed);
            focusB = ClassSkillUi.approach(focusB, f[3], speed);
            float[] g = channels(target.glow);
            glowA = ClassSkillUi.approach(glowA, g[0], speed);
            glowR = ClassSkillUi.approach(glowR, g[1], speed);
            glowG = ClassSkillUi.approach(glowG, g[2], speed);
            glowB = ClassSkillUi.approach(glowB, g[3], speed);
            float[] w = channels(target.wash);
            washA = ClassSkillUi.approach(washA, w[0], speed);
            washR = ClassSkillUi.approach(washR, w[1], speed);
            washG = ClassSkillUi.approach(washG, w[2], speed);
            washB = ClassSkillUi.approach(washB, w[3], speed);
        }

        public int panel() {
            return pack(panelA, panelR, panelG, panelB);
        }

        public int panelDark() {
            return pack(darkA, darkR, darkG, darkB);
        }

        public int accent() {
            return pack(accentA, accentR, accentG, accentB);
        }

        public int focus() {
            return pack(focusA, focusR, focusG, focusB);
        }

        public int glow() {
            return pack(glowA, glowR, glowG, glowB);
        }

        public int wash() {
            return pack(washA, washR, washG, washB);
        }

        private void unpack(int argb, boolean panel) {
            float[] c = channels(argb);
            if (panel) {
                panelA = c[0];
                panelR = c[1];
                panelG = c[2];
                panelB = c[3];
            }
        }

        private void unpackDark(int argb) {
            float[] c = channels(argb);
            darkA = c[0];
            darkR = c[1];
            darkG = c[2];
            darkB = c[3];
        }

        private void unpackAccent(int argb) {
            float[] c = channels(argb);
            accentA = c[0];
            accentR = c[1];
            accentG = c[2];
            accentB = c[3];
        }

        private void unpackFocus(int argb) {
            float[] c = channels(argb);
            focusA = c[0];
            focusR = c[1];
            focusG = c[2];
            focusB = c[3];
        }

        private void unpackGlow(int argb) {
            float[] c = channels(argb);
            glowA = c[0];
            glowR = c[1];
            glowG = c[2];
            glowB = c[3];
        }

        private void unpackWash(int argb) {
            float[] c = channels(argb);
            washA = c[0];
            washR = c[1];
            washG = c[2];
            washB = c[3];
        }

        private static float[] channels(int argb) {
            return new float[] {
                    ((argb >>> 24) & 0xFF) / 255f,
                    ((argb >>> 16) & 0xFF) / 255f,
                    ((argb >>> 8) & 0xFF) / 255f,
                    (argb & 0xFF) / 255f
            };
        }

        private static int pack(float a, float r, float g, float b) {
            return (Mth.clamp((int) (a * 255f), 0, 255) << 24)
                    | (Mth.clamp((int) (r * 255f), 0, 255) << 16)
                    | (Mth.clamp((int) (g * 255f), 0, 255) << 8)
                    | Mth.clamp((int) (b * 255f), 0, 255);
        }
    }
}
