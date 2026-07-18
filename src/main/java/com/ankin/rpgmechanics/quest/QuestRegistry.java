package com.ankin.rpgmechanics.quest;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Server-side live quest definition registry.
 * Play path: datapack/JAR only. Authoring overlay applied only when authoring mode is on.
 */
public final class QuestRegistry {
    private static Map<ResourceLocation, QuestDefinition> datapackQuests = Map.of();
    private static Map<ResourceLocation, QuestDefinition> authoringOverlay = Map.of();
    private static Map<ResourceLocation, QuestDefinition> merged = Map.of();

    private QuestRegistry() {
    }

    public static void setDatapackQuests(Map<ResourceLocation, QuestDefinition> quests) {
        datapackQuests = Map.copyOf(quests);
        rebuild();
        RpgMechanics.LOGGER.info("Loaded {} quest definition(s) from datapacks", datapackQuests.size());
    }

    public static void setAuthoringOverlay(Map<ResourceLocation, QuestDefinition> quests) {
        authoringOverlay = Map.copyOf(quests);
        rebuild();
        RpgMechanics.LOGGER.info("Authoring overlay now has {} quest definition(s)", authoringOverlay.size());
    }

    public static void clearAuthoringOverlay() {
        authoringOverlay = Map.of();
        rebuild();
    }

    private static void rebuild() {
        Map<ResourceLocation, QuestDefinition> next = new LinkedHashMap<>(datapackQuests);
        next.putAll(authoringOverlay);
        merged = Collections.unmodifiableMap(next);
    }

    public static Map<ResourceLocation, QuestDefinition> all() {
        return merged;
    }

    public static Collection<QuestDefinition> values() {
        return merged.values();
    }

    public static Optional<QuestDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(merged.get(id));
    }

    @Nullable
    public static QuestDefinition getOrNull(ResourceLocation id) {
        return merged.get(id);
    }

    public static boolean contains(ResourceLocation id) {
        return merged.containsKey(id);
    }

    public static void putAuthoringQuest(QuestDefinition definition) {
        Map<ResourceLocation, QuestDefinition> next = new LinkedHashMap<>(authoringOverlay);
        next.put(definition.id(), definition);
        setAuthoringOverlay(next);
    }

    public static void removeAuthoringQuest(ResourceLocation id) {
        if (!authoringOverlay.containsKey(id)) {
            return;
        }
        Map<ResourceLocation, QuestDefinition> next = new LinkedHashMap<>(authoringOverlay);
        next.remove(id);
        setAuthoringOverlay(next);
    }

    public static void syncEveryone(MinecraftServer server) {
        QuestSync.syncDefinitionsToAll(server);
    }
}
