package com.ankin.rpgmechanics.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Per-player quest progress. Definitions live in datapacks; only this state is player-bound.
 * {@code completed} is permanent history so finished quests cannot be repeated after leaving the journal.
 */
public record PlayerQuestState(
        List<ResourceLocation> accepted,
        Map<ResourceLocation, Integer> progress,
        Optional<ResourceLocation> tracked,
        List<ResourceLocation> completed
) {
    public static final PlayerQuestState EMPTY = new PlayerQuestState(List.of(), Map.of(), Optional.empty(), List.of());

    public static final Codec<PlayerQuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("accepted", List.of()).forGetter(PlayerQuestState::accepted),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("progress", Map.of()).forGetter(PlayerQuestState::progress),
            ResourceLocation.CODEC.optionalFieldOf("tracked").forGetter(PlayerQuestState::tracked),
            ResourceLocation.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(PlayerQuestState::completed)
    ).apply(instance, PlayerQuestState::new));

    public PlayerQuestState {
        accepted = List.copyOf(accepted);
        progress = Map.copyOf(progress);
        completed = List.copyOf(completed);
    }

    public boolean hasAccepted(ResourceLocation questId) {
        return accepted.contains(questId);
    }

    public boolean hasCompleted(ResourceLocation questId) {
        return completed.contains(questId);
    }

    public int stepIndex(ResourceLocation questId) {
        return progress.getOrDefault(questId, 0);
    }

    public boolean isTracked(ResourceLocation questId) {
        return tracked.isPresent() && tracked.get().equals(questId);
    }

    public PlayerQuestState accept(ResourceLocation questId) {
        if (hasAccepted(questId) || hasCompleted(questId)) {
            return this;
        }
        List<ResourceLocation> nextAccepted = new ArrayList<>(accepted);
        nextAccepted.add(questId);
        Map<ResourceLocation, Integer> nextProgress = new HashMap<>(progress);
        nextProgress.putIfAbsent(questId, 0);
        return new PlayerQuestState(nextAccepted, nextProgress, tracked, completed);
    }

    public PlayerQuestState advance(ResourceLocation questId, int maxStepExclusive) {
        if (!hasAccepted(questId)) {
            return this;
        }
        int current = stepIndex(questId);
        if (current >= maxStepExclusive) {
            return this;
        }
        Map<ResourceLocation, Integer> nextProgress = new HashMap<>(progress);
        nextProgress.put(questId, current + 1);
        return new PlayerQuestState(accepted, nextProgress, tracked, completed);
    }

    public PlayerQuestState markCompleted(ResourceLocation questId) {
        if (hasCompleted(questId)) {
            return this;
        }
        List<ResourceLocation> nextCompleted = new ArrayList<>(completed);
        nextCompleted.add(questId);
        return new PlayerQuestState(accepted, progress, tracked, nextCompleted);
    }

    public PlayerQuestState clearCompleted(ResourceLocation questId) {
        if (!hasCompleted(questId)) {
            return this;
        }
        List<ResourceLocation> nextCompleted = new ArrayList<>(completed);
        nextCompleted.remove(questId);
        return new PlayerQuestState(accepted, progress, tracked, nextCompleted);
    }

    /**
     * Toggle tracking for {@code questId}. Tracking another quest overwrites.
     * Clicking the already-tracked quest clears tracking.
     */
    public PlayerQuestState toggleTrack(ResourceLocation questId) {
        if (!hasAccepted(questId)) {
            return this;
        }
        if (isTracked(questId)) {
            return new PlayerQuestState(accepted, progress, Optional.empty(), completed);
        }
        return new PlayerQuestState(accepted, progress, Optional.of(questId), completed);
    }

    /**
     * Removes a finished quest from the journal.
     * {@code rememberCompletion} true = permanent quest (cannot restart);
     * false = repeatable quest (fully cleared, can restart).
     */
    public PlayerQuestState dismissFromJournal(ResourceLocation questId, boolean rememberCompletion) {
        if (!hasAccepted(questId) && !progress.containsKey(questId) && !isTracked(questId) && !hasCompleted(questId)) {
            return this;
        }
        List<ResourceLocation> nextAccepted = new ArrayList<>(accepted);
        nextAccepted.remove(questId);
        Map<ResourceLocation, Integer> nextProgress = new HashMap<>(progress);
        nextProgress.remove(questId);
        Optional<ResourceLocation> nextTracked = tracked.filter(id -> !id.equals(questId));
        List<ResourceLocation> nextCompleted = new ArrayList<>(completed);
        if (rememberCompletion) {
            if (!nextCompleted.contains(questId)) {
                nextCompleted.add(questId);
            }
        } else {
            nextCompleted.remove(questId);
        }
        return new PlayerQuestState(nextAccepted, nextProgress, nextTracked, nextCompleted);
    }

    /**
     * Drops completion history for quests that are now marked repeatable.
     */
    public PlayerQuestState clearRepeatableCompletions(Set<ResourceLocation> repeatableQuestIds) {
        if (completed.isEmpty() || repeatableQuestIds.isEmpty()) {
            return this;
        }
        List<ResourceLocation> nextCompleted = new ArrayList<>();
        boolean changed = false;
        for (ResourceLocation id : completed) {
            if (repeatableQuestIds.contains(id)) {
                changed = true;
            } else {
                nextCompleted.add(id);
            }
        }
        if (!changed) {
            return this;
        }
        return new PlayerQuestState(accepted, progress, tracked, nextCompleted);
    }

    /**
     * Drops accepted/progress/tracked/completed entries whose quest definition no longer exists.
     * Returns {@code this} when nothing changed.
     */
    public PlayerQuestState purgeMissing(Set<ResourceLocation> knownQuestIds) {
        List<ResourceLocation> nextAccepted = new ArrayList<>();
        for (ResourceLocation id : accepted) {
            if (knownQuestIds.contains(id)) {
                nextAccepted.add(id);
            }
        }
        Map<ResourceLocation, Integer> nextProgress = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : progress.entrySet()) {
            if (knownQuestIds.contains(entry.getKey()) && nextAccepted.contains(entry.getKey())) {
                nextProgress.put(entry.getKey(), entry.getValue());
            }
        }
        Optional<ResourceLocation> nextTracked = tracked.filter(knownQuestIds::contains).filter(nextAccepted::contains);
        List<ResourceLocation> nextCompleted = new ArrayList<>();
        for (ResourceLocation id : completed) {
            if (knownQuestIds.contains(id)) {
                nextCompleted.add(id);
            }
        }
        if (nextAccepted.equals(accepted)
                && nextProgress.equals(progress)
                && nextTracked.equals(tracked)
                && nextCompleted.equals(completed)) {
            return this;
        }
        return new PlayerQuestState(nextAccepted, nextProgress, nextTracked, nextCompleted);
    }

    public Set<ResourceLocation> acceptedSet() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(accepted));
    }
}
