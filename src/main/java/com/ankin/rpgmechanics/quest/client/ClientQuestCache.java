package com.ankin.rpgmechanics.quest.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ankin.rpgmechanics.quest.PlayerQuestState;
import com.ankin.rpgmechanics.quest.QuestDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientQuestCache {
    private static Map<ResourceLocation, QuestDefinition> definitions = Map.of();
    private static PlayerQuestState playerState = PlayerQuestState.EMPTY;

    private ClientQuestCache() {
    }

    public static void setDefinitions(List<QuestDefinition> quests) {
        Map<ResourceLocation, QuestDefinition> next = new LinkedHashMap<>();
        for (QuestDefinition quest : quests) {
            next.put(quest.id(), quest);
        }
        definitions = Collections.unmodifiableMap(next);
        refreshOpenBook();
    }

    public static void setPlayerState(PlayerQuestState state) {
        playerState = state == null ? PlayerQuestState.EMPTY : state;
        refreshOpenBook();
    }

    private static void refreshOpenBook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof QuestBookScreen book) {
            book.refreshFromCache();
        }
        if (minecraft.screen instanceof QuestEditorScreen editor) {
            editor.refreshFromCache();
        }
    }

    public static Collection<QuestDefinition> definitions() {
        return definitions.values();
    }

    public static Optional<QuestDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static PlayerQuestState playerState() {
        return playerState;
    }

    public static void clear() {
        definitions = Map.of();
        playerState = PlayerQuestState.EMPTY;
    }
}
