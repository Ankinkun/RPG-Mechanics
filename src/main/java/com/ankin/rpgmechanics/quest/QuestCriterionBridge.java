package com.ankin.rpgmechanics.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bridges vanilla advancement criterion triggers into quest step progression.
 * Unaccepted quests listen only on step 0 (initiation). Accepted quests listen
 * on the current step. Final step completion finishes the quest.
 */
public final class QuestCriterionBridge {
    private QuestCriterionBridge() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onCriterionTrigger(SimpleCriterionTrigger<?> trigger, ServerPlayer player, Predicate<?> test) {
        PlayerQuestState state = QuestManager.get(player);
        record Match(ResourceLocation questId, int stepIndex) {
        }
        List<Match> toProgress = new ArrayList<>();

        for (QuestDefinition definition : QuestRegistry.values()) {
            ResourceLocation questId = definition.id();
            if (!definition.hasSteps()) {
                continue;
            }

            if (state.hasCompleted(questId)) {
                continue;
            }

            final int stepIndex;
            if (state.hasAccepted(questId)) {
                stepIndex = state.stepIndex(questId);
                if (definition.isComplete(stepIndex)) {
                    continue;
                }
            } else {
                // Not started — ONLY the first step may initiate. Later steps are ignored.
                stepIndex = 0;
            }

            QuestStep step = definition.stepAt(stepIndex);
            if (step == null) {
                continue;
            }
            Criterion<?> criterion = step.criterion();
            if (criterion.trigger() != trigger) {
                continue;
            }
            CriterionTriggerInstance instance = criterion.triggerInstance();
            if (((Predicate) test).test(instance)) {
                toProgress.add(new Match(questId, stepIndex));
            }
        }

        for (Match match : toProgress) {
            QuestProgressEvent event = QuestManager.progress(player, match.questId(), match.stepIndex());
            if (event != QuestProgressEvent.NONE) {
                RpgMechanics.LOGGER.debug(
                        "Quest {} {} (step {}) for {} via criterion {}",
                        match.questId(),
                        event,
                        match.stepIndex(),
                        player.getGameProfile().getName(),
                        trigger
                );
            }
        }
    }
}
