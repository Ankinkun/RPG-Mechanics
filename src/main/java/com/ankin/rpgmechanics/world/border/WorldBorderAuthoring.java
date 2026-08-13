package com.ankin.rpgmechanics.world.border;

import java.util.Optional;

import com.ankin.rpgmechanics.world.border.network.WorldBorderSync;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared draft edit path for commands + border wand.
 */
public final class WorldBorderAuthoring {
    private WorldBorderAuthoring() {
    }

    public static boolean canAuthor(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    public static Component addVertex(ServerPlayer player, double x, double z) {
        ResourceLocation id = player.level().dimension().location();
        BorderPolygon draft = WorldBorderState.draftOrEmpty(id).withVertex(new BorderPolygon.Vertex(x, z));
        WorldBorderState.setDraft(id, draft);
        WorldBorderSync.syncDraft(player.server, id);
        Component base = Component.literal("Draft vertex #" + draft.vertices().size()
                + " at " + (int) x + ", " + (int) z);
        Optional<String> warn = draft.validationMessage();
        if (warn.isPresent() && draft.vertices().size() >= 3) {
            return base.copy().append(Component.literal(" — WARNING: " + warn.get()));
        }
        return base;
    }

    public static Component undo(ServerPlayer player) {
        ResourceLocation id = player.level().dimension().location();
        BorderPolygon draft = WorldBorderState.draftOrEmpty(id).withoutLastVertex();
        if (draft.vertices().isEmpty()) {
            WorldBorderState.clearDraft(id);
        } else {
            WorldBorderState.setDraft(id, draft);
        }
        WorldBorderSync.syncDraft(player.server, id);
        return Component.literal("Draft vertices: " + draft.vertices().size());
    }

    public static Component clear(ServerPlayer player) {
        ResourceLocation id = player.level().dimension().location();
        WorldBorderState.clearDraft(id);
        WorldBorderSync.syncDraft(player.server, id);
        return Component.literal("Cleared draft for " + id);
    }
}
