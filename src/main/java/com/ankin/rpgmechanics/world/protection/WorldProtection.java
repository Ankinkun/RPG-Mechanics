package com.ankin.rpgmechanics.world.protection;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * RPG terrain lock policy: block modification denied unless allowlisted
 * (and optionally OP when {@code opsBypassProtection} is true).
 */
public final class WorldProtection {
    private static final long DENY_MESSAGE_COOLDOWN_MS = 1500L;
    private static final Map<UUID, Long> lastDenyMessageMs = new HashMap<>();

    private WorldProtection() {
    }

    public static boolean isEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.protectionEnabled.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static boolean opsBypass() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return false;
        }
        try {
            return RpgMechanicsConfig.SERVER.opsBypassProtection.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public static boolean canModifyBlocks(@Nullable Entity actor, Level level) {
        if (!isEnabled() || level.isClientSide()) {
            return true;
        }
        if (actor instanceof ServerPlayer player) {
            return isBuilder(player);
        }
        if (actor instanceof Player) {
            return false;
        }
        // Mobs, explosions without player, pistons, fire, etc.
        return false;
    }

    public static boolean isBuilder(ServerPlayer player) {
        if (opsBypass() && player.hasPermissions(2)) {
            return true;
        }
        return isOnAllowlist(player);
    }

    public static boolean isOnAllowlist(ServerPlayer player) {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return false;
        }
        List<? extends String> allowlist;
        try {
            allowlist = RpgMechanicsConfig.SERVER.builderAllowlist.get();
        } catch (IllegalStateException exception) {
            return false;
        }
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

    public static void notifyDenied(@Nullable Entity actor) {
        if (!(actor instanceof ServerPlayer player)) {
            return;
        }
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return;
        }
        try {
            if (!RpgMechanicsConfig.SERVER.denyMessage.get()) {
                return;
            }
        } catch (IllegalStateException exception) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastDenyMessageMs.get(player.getUUID());
        if (previous != null && now - previous < DENY_MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastDenyMessageMs.put(player.getUUID(), now);
        player.displayClientMessage(Component.translatable("message.rpgmechanics.world_protected"), true);
    }
}
