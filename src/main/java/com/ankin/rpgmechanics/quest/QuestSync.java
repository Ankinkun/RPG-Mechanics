package com.ankin.rpgmechanics.quest;

import java.util.ArrayList;
import java.util.List;

import com.ankin.rpgmechanics.quest.network.SyncPlayerQuestStatePayload;
import com.ankin.rpgmechanics.quest.network.SyncQuestDefinitionsPayload;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestSync {
    private QuestSync() {
    }

    public static void syncDefinitionsToPlayer(ServerPlayer player) {
        List<QuestDefinition> definitions = new ArrayList<>(QuestRegistry.values());
        PacketDistributor.sendToPlayer(player, new SyncQuestDefinitionsPayload(definitions));
    }

    public static void syncDefinitionsToAll(MinecraftServer server) {
        List<QuestDefinition> definitions = new ArrayList<>(QuestRegistry.values());
        SyncQuestDefinitionsPayload payload = new SyncQuestDefinitionsPayload(definitions);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void syncPlayerState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPlayerQuestStatePayload(QuestManager.get(player)));
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        syncDefinitionsToPlayer(player);
        syncPlayerState(player);
    }
}
