package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.quest.client.QuestClientPayloadHandlers;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Loaded only when {@link net.neoforged.fml.loading.FMLEnvironment#dist} is client.
 */
public final class QuestClientNetworkBootstrap {
    private QuestClientNetworkBootstrap() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                SyncQuestDefinitionsPayload.TYPE,
                SyncQuestDefinitionsPayload.STREAM_CODEC,
                QuestClientPayloadHandlers::handleDefinitions
        );
        registrar.playToClient(
                SyncPlayerQuestStatePayload.TYPE,
                SyncPlayerQuestStatePayload.STREAM_CODEC,
                QuestClientPayloadHandlers::handlePlayerState
        );
        registrar.playToClient(
                OpenQuestEditorPayload.TYPE,
                OpenQuestEditorPayload.STREAM_CODEC,
                QuestClientPayloadHandlers::handleOpenEditor
        );
        registrar.playToClient(
                ShowQuestToastPayload.TYPE,
                ShowQuestToastPayload.STREAM_CODEC,
                QuestClientPayloadHandlers::handleToast
        );
    }
}
