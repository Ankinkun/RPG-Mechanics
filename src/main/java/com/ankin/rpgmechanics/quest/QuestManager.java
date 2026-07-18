package com.ankin.rpgmechanics.quest;

import com.ankin.rpgmechanics.quest.network.ShowQuestToastPayload;
import com.ankin.rpgmechanics.registry.ModAttachments;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestManager {
    private QuestManager() {
    }

    public static PlayerQuestState get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_QUESTS);
    }

    public static void set(ServerPlayer player, PlayerQuestState state) {
        player.setData(ModAttachments.PLAYER_QUESTS, state);
        QuestSync.syncPlayerState(player);
    }

    public static boolean accept(ServerPlayer player, ResourceLocation questId) {
        if (!QuestRegistry.contains(questId)) {
            player.sendSystemMessage(Component.literal("Unknown quest: " + questId));
            return false;
        }
        PlayerQuestState current = get(player);
        if (current.hasCompleted(questId)) {
            player.sendSystemMessage(Component.translatable("message.rpgmechanics.quest.already_completed", questId.toString()));
            return false;
        }
        if (current.hasAccepted(questId)) {
            return false;
        }
        set(player, current.accept(questId));
        player.sendSystemMessage(Component.translatable("message.rpgmechanics.quest.accepted", questId.toString()));
        return true;
    }

    /**
     * Advances an already-accepted quest by one step (commands / testing).
     */
    public static boolean advance(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestState current = get(player);
        if (!current.hasAccepted(questId)) {
            return false;
        }
        return progress(player, questId, current.stepIndex(questId)) != QuestProgressEvent.NONE;
    }

    /**
     * Applies first-step initiation or normal step progress.
     * {@code matchedStepIndex} must be the step whose criterion fired.
     * Unaccepted quests may only progress when {@code matchedStepIndex == 0}.
     */
    public static QuestProgressEvent progress(ServerPlayer player, ResourceLocation questId, int matchedStepIndex) {
        QuestDefinition definition = QuestRegistry.getOrNull(questId);
        if (definition == null || !definition.hasSteps()) {
            return QuestProgressEvent.NONE;
        }

        PlayerQuestState current = get(player);
        if (current.hasCompleted(questId)) {
            return QuestProgressEvent.NONE;
        }
        int max = definition.steps().size();
        boolean wasAccepted = current.hasAccepted(questId);

        if (!wasAccepted) {
            // Hard guard: later steps must never initiate a quest.
            if (matchedStepIndex != 0) {
                return QuestProgressEvent.NONE;
            }
        } else {
            int before = current.stepIndex(questId);
            if (before >= max || matchedStepIndex != before) {
                return QuestProgressEvent.NONE;
            }
        }

        int before = wasAccepted ? current.stepIndex(questId) : 0;
        if (wasAccepted && before >= max) {
            return QuestProgressEvent.NONE;
        }

        PlayerQuestState next = wasAccepted ? current : current.accept(questId);
        next = next.advance(questId, max);
        int after = next.stepIndex(questId);
        if (after >= max && !definition.repeatable()) {
            next = next.markCompleted(questId);
        }
        set(player, next);

        QuestProgressEvent event;
        if (!wasAccepted) {
            event = after >= max ? QuestProgressEvent.COMPLETED : QuestProgressEvent.INITIATED;
        } else if (after >= max) {
            event = QuestProgressEvent.COMPLETED;
        } else {
            event = QuestProgressEvent.STEP_COMPLETED;
        }

        QuestStep completedStep = definition.stepAt(before);
        sendToast(player, definition, completedStep, event);
        return event;
    }

    private static void sendToast(ServerPlayer player, QuestDefinition quest, QuestStep completedStep, QuestProgressEvent event) {
        String stepTitle = completedStep != null ? completedStep.title() : quest.title();
        PacketDistributor.sendToPlayer(player, new ShowQuestToastPayload(
                quest.id(),
                event,
                quest.title(),
                stepTitle,
                quest.icon().orElse(null)
        ));
    }

    public static boolean toggleTrack(ServerPlayer player, ResourceLocation questId) {
        if (!QuestRegistry.contains(questId)) {
            return false;
        }
        PlayerQuestState current = get(player);
        if (!current.hasAccepted(questId)) {
            return false;
        }
        set(player, current.toggleTrack(questId));
        return true;
    }

    /**
     * Removes a finished quest from the journal after the player confirms Complete.
     * Permanent quests stay completed on the character; repeatable quests clear fully.
     */
    public static boolean dismissCompleted(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestState current = get(player);
        if (!current.hasAccepted(questId)) {
            return false;
        }
        QuestDefinition definition = QuestRegistry.getOrNull(questId);
        if (definition == null || !definition.isComplete(current.stepIndex(questId))) {
            return false;
        }
        set(player, current.dismissFromJournal(questId, !definition.repeatable()));
        return true;
    }

    /** Strips missing definitions and drops completion history for quests marked repeatable. */
    public static void purgeMissingDefinitions(ServerPlayer player) {
        PlayerQuestState current = get(player);
        PlayerQuestState next = current.purgeMissing(QuestRegistry.all().keySet());
        Set<ResourceLocation> repeatableIds = new HashSet<>();
        for (QuestDefinition definition : QuestRegistry.values()) {
            if (definition.repeatable()) {
                repeatableIds.add(definition.id());
            }
        }
        next = next.clearRepeatableCompletions(repeatableIds);
        if (next != current) {
            set(player, next);
        }
    }

    public static void purgeMissingDefinitionsForAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgeMissingDefinitions(player);
        }
    }
}
