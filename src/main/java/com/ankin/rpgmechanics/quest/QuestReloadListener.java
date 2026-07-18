package com.ankin.rpgmechanics.quest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads pack-owned quest definitions from {@code data/<namespace>/rpgmechanics/quests/*.json}.
 */
public final class QuestReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String DIRECTORY = "rpgmechanics/quests";

    public QuestReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var ops = server != null
                ? RegistryOps.create(JsonOps.INSTANCE, server.registryAccess())
                : JsonOps.INSTANCE;

        Map<ResourceLocation, QuestDefinition> loaded = new HashMap<>();
        object.forEach((fileId, json) -> {
            try {
                QuestDefinition parsed = QuestDefinition.CODEC.parse(ops, json)
                        .getOrThrow(message -> new IllegalStateException(message));
                ResourceLocation id = parsed.id();
                if (loaded.containsKey(id)) {
                    LOGGER.warn("Duplicate quest id {} from file {}; overwriting", id, fileId);
                }
                loaded.put(id, parsed);
            } catch (Exception exception) {
                LOGGER.error("Failed to parse quest file {}", fileId, exception);
            }
        });
        QuestRegistry.setDatapackQuests(loaded);

        if (isAuthoringEnabledSafe()) {
            if (server != null) {
                QuestAuthoringIO.reloadOverlayFromDisk(server.registryAccess());
            } else {
                QuestAuthoringIO.reloadOverlayFromDisk();
            }
        } else {
            QuestRegistry.clearAuthoringOverlay();
        }

        RpgMechanics.LOGGER.info("Quest definitions ready: {} total", QuestRegistry.all().size());

        if (server != null) {
            QuestManager.purgeMissingDefinitionsForAll(server);
            QuestSync.syncDefinitionsToAll(server);
        }
    }

    private static boolean isAuthoringEnabledSafe() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return false;
        }
        try {
            return RpgMechanicsConfig.SERVER.questAuthoringMode.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
