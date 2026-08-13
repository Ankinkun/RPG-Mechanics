package com.ankin.rpgmechanics.world.border;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ankin.rpgmechanics.RpgMechanics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Loads/saves per-dimension border JSON under config/rpgmechanics/world/borders/.
 */
public final class WorldBorderIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private WorldBorderIO() {
    }

    public static Path bordersDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(RpgMechanics.MOD_ID).resolve("world").resolve("borders");
    }

    public static Path fileFor(ResourceLocation dimension) {
        String fileName = dimension.getNamespace() + "_" + dimension.getPath().replace('/', '_') + ".json";
        return bordersDirectory().resolve(fileName);
    }

    public static Map<ResourceLocation, WorldBorderDefinition> loadAll() {
        Map<ResourceLocation, WorldBorderDefinition> loaded = new HashMap<>();
        Path dir = bordersDirectory();
        if (!Files.isDirectory(dir)) {
            return loaded;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    Optional<WorldBorderDefinition> definition = read(path);
                    definition.ifPresent(def -> {
                        if (def.polygon().isValid()) {
                            loaded.put(def.dimension(), def);
                        } else {
                            RpgMechanics.LOGGER.warn("Ignoring invalid world border polygon in {}", path);
                        }
                    });
                } catch (Exception exception) {
                    RpgMechanics.LOGGER.error("Failed to load world border {}", path, exception);
                }
            });
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to list world border directory {}", dir, exception);
        }
        return loaded;
    }

    public static Optional<WorldBorderDefinition> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return WorldBorderDefinition.FILE_CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(message -> RpgMechanics.LOGGER.error("World border parse error in {}: {}", path, message));
        }
    }

    public static void save(WorldBorderDefinition definition) throws IOException {
        Path path = fileFor(definition.dimension());
        Files.createDirectories(path.getParent());
        JsonElement element = WorldBorderDefinition.FILE_CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IOException("Failed to encode border: " + message));
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(element, writer);
        }
    }

    public static boolean delete(ResourceLocation dimension) throws IOException {
        Path path = fileFor(dimension);
        return Files.deleteIfExists(path);
    }

    public static void ensureDirectory(MinecraftServer ignored) throws IOException {
        Files.createDirectories(bordersDirectory());
    }
}
