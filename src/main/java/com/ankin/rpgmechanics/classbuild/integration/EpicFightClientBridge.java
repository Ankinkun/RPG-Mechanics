package com.ankin.rpgmechanics.classbuild.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Hard client Epic Fight bridge (compileOnly).
 */
public final class EpicFightClientBridge {
    private EpicFightClientBridge() {
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
