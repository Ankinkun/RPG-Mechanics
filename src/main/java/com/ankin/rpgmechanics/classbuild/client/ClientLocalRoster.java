package com.ankin.rpgmechanics.classbuild.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.CharacterRoster;
import com.ankin.rpgmechanics.classbuild.CharacterSlot;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Client-side roster for title-screen character select (before a world/player exists).
 */
public final class ClientLocalRoster {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static CharacterRoster cached = CharacterRoster.EMPTY;

    private ClientLocalRoster() {
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(RpgMechanics.MOD_ID).resolve("characters").resolve("roster.json");
    }

    public static CharacterRoster get() {
        return cached;
    }

    public static void load() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            cached = CharacterRoster.EMPTY;
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            cached = CharacterRoster.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(message -> new IllegalStateException(message));
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to load local character roster", exception);
            cached = CharacterRoster.EMPTY;
        }
    }

    public static void save(CharacterRoster roster) {
        cached = roster == null ? CharacterRoster.EMPTY : roster;
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            JsonElement json = CharacterRoster.CODEC.encodeStart(JsonOps.INSTANCE, cached)
                    .getOrThrow(message -> new IllegalStateException(message));
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to save local character roster", exception);
        }
    }

    public static void putSlot(int index, CharacterSlot slot) {
        CharacterRoster roster = cached.withSlot(index, slot);
        save(roster);
    }

    public static void deleteSlot(int index) {
        save(cached.withSlot(index, CharacterSlot.EMPTY).clearActive());
    }

    public static void createFromDraft(int index, ClassBuildState draft) {
        ClassBuildState confirmed = draft.confirmedCopy();
        putSlot(index, CharacterSlot.create("Guardian", confirmed));
    }
}
