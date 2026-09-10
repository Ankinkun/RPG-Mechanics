package com.ankin.rpgmechanics.classbuild.client.ui;

import com.ankin.rpgmechanics.classbuild.integration.EpicFightClientSoft;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Fallback: fire Epic Fight skill GUI key one tick after the hub closes. */
public final class RpgHubTabBarSkillsTick {
    private static boolean pending;
    private static int attempts;
    private static boolean verifyOpen;

    private RpgHubTabBarSkillsTick() {
    }

    public static void request() {
        pending = true;
        attempts = 0;
        verifyOpen = false;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (verifyOpen) {
            verifyOpen = false;
            if (minecraft.screen == null) {
                RpgHubTabBar.hintSkillsFailed(minecraft, "skill_gui key click produced no screen");
            }
            return;
        }
        if (!pending) {
            return;
        }
        if (minecraft.player == null) {
            pending = false;
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        attempts++;
        if (EpicFightClientSoft.openSkillEditScreen()) {
            pending = false;
            return;
        }
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (!"key.epicfight.skill_gui".equals(mapping.getName())) {
                continue;
            }
            InputConstants.Key key = mapping.getKey();
            if (key == InputConstants.UNKNOWN) {
                mapping.setKey(InputConstants.getKey("key.keyboard.k"));
                KeyMapping.resetMapping();
                key = mapping.getKey();
            }
            if (key != InputConstants.UNKNOWN) {
                KeyMapping.click(key);
            }
            pending = false;
            verifyOpen = true;
            return;
        }
        if (attempts >= 3) {
            pending = false;
            RpgHubTabBar.hintSkillsFailed(minecraft, "skill_gui binding missing");
        }
    }
}
