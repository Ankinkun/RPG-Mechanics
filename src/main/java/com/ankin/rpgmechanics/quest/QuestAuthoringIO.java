package com.ankin.rpgmechanics.quest;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.ankin.rpgmechanics.RpgMechanics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Authoring-only disk export under {@code config/rpgmechanics/quest_export/}.
 * Criterion JSON must be read/written with {@link RegistryOps} or item/block predicates strip to empty.
 */
public final class QuestAuthoringIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private QuestAuthoringIO() {
    }

    public static Path exportDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(RpgMechanics.MOD_ID).resolve("quest_export");
        try {
            Files.createDirectories(dir);
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to create quest export directory {}", dir, exception);
        }
        return dir;
    }

    public static void reloadOverlayFromDisk() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            reloadOverlayFromDisk(server.registryAccess());
            return;
        }
        // No server yet — load with plain JsonOps as a best-effort; server-start will re-load with registries.
        reloadOverlayFromDisk(null);
    }

    public static void reloadOverlayFromDisk(HolderLookup.Provider registries) {
        Path dir = exportDirectory();
        Map<ResourceLocation, QuestDefinition> loaded = new HashMap<>();
        if (!Files.isDirectory(dir)) {
            QuestRegistry.setAuthoringOverlay(loaded);
            return;
        }
        var ops = registries != null ? RegistryOps.create(JsonOps.INSTANCE, registries) : JsonOps.INSTANCE;
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    QuestDefinition definition = QuestDefinition.CODEC.parse(ops, json)
                            .getOrThrow(message -> new IllegalStateException(message));
                    loaded.put(definition.id(), definition);
                } catch (Exception exception) {
                    RpgMechanics.LOGGER.error("Failed to load authoring quest {}", path, exception);
                }
            });
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to list authoring quests in {}", dir, exception);
        }
        QuestRegistry.setAuthoringOverlay(loaded);
        RpgMechanics.LOGGER.info("Authoring overlay loaded {} quest(s) from {}", loaded.size(), dir);
    }

    public static void save(QuestDefinition definition, HolderLookup.Provider registries) throws IOException {
        Path dir = exportDirectory();
        String fileName = safeFileName(definition.id()) + ".json";
        Path target = dir.resolve(fileName);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        JsonElement json = QuestDefinition.CODEC.encodeStart(ops, definition)
                .getOrThrow(message -> new IllegalStateException(message));
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
        QuestRegistry.putAuthoringQuest(definition);
    }

    public static void delete(ResourceLocation id) throws IOException {
        Path dir = exportDirectory();
        Path target = dir.resolve(safeFileName(id) + ".json");
        Files.deleteIfExists(target);
        QuestRegistry.removeAuthoringQuest(id);
    }

    private static String safeFileName(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }
}
