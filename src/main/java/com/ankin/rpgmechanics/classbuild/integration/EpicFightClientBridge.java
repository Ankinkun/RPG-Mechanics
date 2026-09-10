package com.ankin.rpgmechanics.classbuild.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Hard client Epic Fight bridge (compileOnly).
 */
public final class EpicFightClientBridge {
    private EpicFightClientBridge() {
    }

    public static boolean openSkillEditScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (patch == null || patch.getPlayerSkills() == null) {
            return false;
        }
        minecraft.setScreen(new SkillEditScreen(player, patch.getPlayerSkills()));
        return minecraft.screen instanceof SkillEditScreen;
    }

    public static void ensureCombatMode() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (patch == null) {
            return;
        }
        if (!patch.isEpicFightMode()) {
            patch.toEpicFightMode(true);
        }
    }
}
