package com.ankin.rpgmechanics.quest.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class QuestNetwork {
    private QuestNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ToggleTrackQuestPayload.TYPE,
                ToggleTrackQuestPayload.STREAM_CODEC,
                QuestServerPayloadHandler::handleToggleTrack
        );
        registrar.playToServer(
                DismissCompletedQuestPayload.TYPE,
                DismissCompletedQuestPayload.STREAM_CODEC,
                QuestServerPayloadHandler::handleDismissCompleted
        );
        registrar.playToServer(
                EditorSaveQuestPayload.TYPE,
                EditorSaveQuestPayload.STREAM_CODEC,
                QuestServerPayloadHandler::handleSave
        );
        registrar.playToServer(
                EditorDeleteQuestPayload.TYPE,
                EditorDeleteQuestPayload.STREAM_CODEC,
                QuestServerPayloadHandler::handleDelete
        );
        registrar.playToServer(
                RequestOpenQuestEditorPayload.TYPE,
                RequestOpenQuestEditorPayload.STREAM_CODEC,
                QuestServerPayloadHandler::handleOpenRequest
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            QuestClientNetworkBootstrap.register(registrar);
        } else {
            registrar.playToClient(SyncQuestDefinitionsPayload.TYPE, SyncQuestDefinitionsPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(SyncPlayerQuestStatePayload.TYPE, SyncPlayerQuestStatePayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(OpenQuestEditorPayload.TYPE, OpenQuestEditorPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(ShowQuestToastPayload.TYPE, ShowQuestToastPayload.STREAM_CODEC, (payload, context) -> {
            });
        }
    }
}
