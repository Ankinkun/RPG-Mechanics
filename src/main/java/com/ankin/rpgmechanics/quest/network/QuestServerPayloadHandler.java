package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.quest.QuestManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class QuestServerPayloadHandler {
    private QuestServerPayloadHandler() {
    }

    public static void handleToggleTrack(ToggleTrackQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                QuestManager.toggleTrack(serverPlayer, payload.questId());
            }
        });
    }

    public static void handleDismissCompleted(DismissCompletedQuestPayload payload, IPayloadContext context) {
        DismissCompletedQuestPayload.handle(payload, context);
    }

    public static void handleSave(EditorSaveQuestPayload payload, IPayloadContext context) {
        EditorSaveQuestPayload.handle(payload, context);
    }

    public static void handleDelete(EditorDeleteQuestPayload payload, IPayloadContext context) {
        EditorDeleteQuestPayload.handle(payload, context);
    }

    public static void handleOpenRequest(RequestOpenQuestEditorPayload payload, IPayloadContext context) {
        RequestOpenQuestEditorPayload.handleServer(payload, context);
    }
}
