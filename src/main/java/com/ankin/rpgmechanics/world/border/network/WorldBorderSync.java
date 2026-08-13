package com.ankin.rpgmechanics.world.border.network;

import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;
import com.ankin.rpgmechanics.world.border.WorldBorderState;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WorldBorderSync {
    private WorldBorderSync() {
    }

    public static void syncAll(MinecraftServer server) {
        if (!WorldBorderState.isBorderFeatureEnabled()) {
            PacketDistributor.sendToAllPlayers(SyncWorldBorderPayload.featureOff());
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            WorldBorderState.syncToPlayer(player);
        }
    }

    public static void syncDefinition(MinecraftServer server, WorldBorderDefinition definition) {
        PacketDistributor.sendToAllPlayers(SyncWorldBorderPayload.from(definition));
    }

    public static void syncDraft(MinecraftServer server, ResourceLocation dimension) {
        BorderPolygon draft = WorldBorderState.draft(dimension).orElse(null);
        SyncBorderDraftPayload payload = draft == null || draft.vertices().isEmpty()
                ? SyncBorderDraftPayload.clear(dimension)
                : SyncBorderDraftPayload.of(dimension, draft);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void syncDraftToPlayer(ServerPlayer player) {
        ResourceLocation dimension = player.level().dimension().location();
        BorderPolygon draft = WorldBorderState.draft(dimension).orElse(null);
        SyncBorderDraftPayload payload = draft == null || draft.vertices().isEmpty()
                ? SyncBorderDraftPayload.clear(dimension)
                : SyncBorderDraftPayload.of(dimension, draft);
        PacketDistributor.sendToPlayer(player, payload);
    }
}
