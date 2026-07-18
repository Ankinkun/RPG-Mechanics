package com.ankin.rpgmechanics.quest.client;

import com.ankin.rpgmechanics.quest.network.OpenQuestEditorPayload;
import com.ankin.rpgmechanics.quest.network.ShowQuestToastPayload;
import com.ankin.rpgmechanics.quest.network.SyncPlayerQuestStatePayload;
import com.ankin.rpgmechanics.quest.network.SyncQuestDefinitionsPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class QuestClientPayloadHandlers {
    private QuestClientPayloadHandlers() {
    }

    public static void handleDefinitions(SyncQuestDefinitionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestCache.setDefinitions(payload.quests()));
    }

    public static void handlePlayerState(SyncPlayerQuestStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestCache.setPlayerState(payload.state()));
    }

    public static void handleOpenEditor(OpenQuestEditorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new QuestEditorScreen()));
    }

    public static void handleToast(ShowQuestToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> QuestToastHelper.show(
                payload.event(),
                payload.questTitle(),
                payload.stepTitle(),
                payload.icon()
        ));
    }
}
