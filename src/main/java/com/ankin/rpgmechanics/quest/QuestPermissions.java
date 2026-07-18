package com.ankin.rpgmechanics.quest;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.server.level.ServerPlayer;

public final class QuestPermissions {
    private QuestPermissions() {
    }

    public static boolean isAuthoringEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return false;
        }
        try {
            return RpgMechanicsConfig.SERVER.questAuthoringMode.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public static boolean canUseEditor(ServerPlayer player) {
        if (!isAuthoringEnabled()) {
            return false;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        List<? extends String> allowlist = RpgMechanicsConfig.SERVER.questDevAllowlist.get();
        String name = player.getGameProfile().getName();
        UUID uuid = player.getUUID();
        String uuidString = uuid.toString();
        for (String entry : allowlist) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            if (trimmed.equalsIgnoreCase(name) || trimmed.equalsIgnoreCase(uuidString)) {
                return true;
            }
            try {
                if (UUID.fromString(trimmed).equals(uuid)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // not a UUID
            }
            if (trimmed.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
