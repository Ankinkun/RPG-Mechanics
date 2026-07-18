package com.ankin.rpgmechanics.keybind;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ankin.rpgmechanics.RpgMechanics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Loads/saves keybind profiles under {@code config/rpgmechanics/keybinds/}.
 * <ul>
 *   <li>{@code pack_defaults.json} — pack-owned defaults (vanilla + mods seeded on first run)</li>
 *   <li>{@code player.json} — player overlay (source of truth for edits)</li>
 * </ul>
 */
public final class KeybindProfileIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String PACK_DEFAULTS = "pack_defaults.json";
    private static final String PLAYER = "player.json";

    private KeybindProfileIO() {
    }

    public static Path directory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(RpgMechanics.MOD_ID).resolve("keybinds");
        try {
            Files.createDirectories(dir);
        } catch (IOException exception) {
            RpgMechanics.LOGGER.error("Failed to create keybind config directory {}", dir, exception);
        }
        return dir;
    }

    /**
     * Ensures {@code pack_defaults.json} contains every discovered KeyMapping and vanilla category.
     * Creates the file when missing; merges in newly discovered bindings without overwriting edits.
     */
    public static void ensurePackDefaultsSeeded() {
        KeybindCatalog.refresh();
        if (KeybindCatalog.all().isEmpty()) {
            return;
        }
        Path path = directory().resolve(PACK_DEFAULTS);
        KeybindProfile seed = createVanillaRegistrySeed();
        Optional<KeybindProfile> existing = loadFile(path);
        if (existing.isEmpty()) {
            saveFile(path, seed);
            RpgMechanics.LOGGER.info(
                    "Wrote vanilla keybind registry ({} bindings, {} categories) to {}",
                    seed.bindings().size(),
                    seed.categories().size(),
                    path
            );
            return;
        }
        MergeResult merged = mergeMissingFromSeed(existing.get(), seed);
        KeybindProfile pack = merged.profile();
        Optional<KeybindProfile> refreshed = refreshUnresolvedCategoryTitles(pack);
        if (refreshed.isPresent()) {
            pack = refreshed.get();
            merged = new MergeResult(pack, true, merged.addedCount());
        }
        if (merged.dirty()) {
            saveFile(path, pack);
            RpgMechanics.LOGGER.info(
                    "Updated pack_defaults.json with {} new binding(s) / categor(ies) (titles refreshed={})",
                    merged.addedCount(),
                    refreshed.isPresent()
            );
        }
    }

    /**
     * Snapshot of all registered KeyMappings grouped by their vanilla/mod category ids.
     */
    public static KeybindProfile createVanillaRegistrySeed() {
        Map<String, List<String>> entriesByCategory = new LinkedHashMap<>();
        Map<String, BindingOverride> bindings = new LinkedHashMap<>();

        for (KeyMapping mapping : KeybindCatalog.all()) {
            String categoryId = mapping.getCategory();
            entriesByCategory.computeIfAbsent(categoryId, ignored -> new ArrayList<>()).add(mapping.getName());
            String defaultKey = KeybindCatalog.defaultKeyName(mapping);
            BindingOverride override = BindingOverride.defaultsFromVanilla(defaultKey)
                    .withCustomCategory(Optional.of(categoryId));
            bindings.put(mapping.getName(), override);
        }

        List<CategoryDef> categories = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : entriesByCategory.entrySet()) {
            // Keep id as the lang key; resolve display names live via I18n (do not bake unresolved keys).
            String id = entry.getKey();
            String title = KeybindCatalog.translateKey(id, id);
            categories.add(new CategoryDef(id, title, entry.getValue()));
        }
        return new KeybindProfile(categories, bindings);
    }

    /**
     * When pack category titles were baked as raw ids (lang not ready at first seed), refresh them
     * once translations exist — without overwriting intentional custom titles.
     */
    public static Optional<KeybindProfile> refreshUnresolvedCategoryTitles(KeybindProfile profile) {
        boolean dirty = false;
        List<CategoryDef> next = new ArrayList<>();
        for (CategoryDef category : profile.categories()) {
            if (category.title().equals(category.id()) && I18n.exists(category.id())) {
                next.add(category.withTitle(I18n.get(category.id())));
                dirty = true;
            } else {
                next.add(category);
            }
        }
        return dirty ? Optional.of(new KeybindProfile(next, profile.bindings())) : Optional.empty();
    }

    private static MergeResult mergeMissingFromSeed(KeybindProfile pack, KeybindProfile seed) {
        Map<String, CategoryDef> categories = new LinkedHashMap<>();
        for (CategoryDef category : seed.categories()) {
            categories.put(category.id(), category);
        }
        for (CategoryDef category : pack.categories()) {
            categories.put(category.id(), category);
        }

        int added = 0;
        for (CategoryDef seedCategory : seed.categories()) {
            CategoryDef existing = categories.get(seedCategory.id());
            if (existing == null) {
                categories.put(seedCategory.id(), seedCategory);
                added++;
                continue;
            }
            List<String> entries = new ArrayList<>(existing.entries());
            boolean changed = false;
            for (String bindingName : seedCategory.entries()) {
                if (!entries.contains(bindingName)) {
                    entries.add(bindingName);
                    changed = true;
                    added++;
                }
            }
            if (changed) {
                categories.put(seedCategory.id(), existing.withEntries(entries));
            }
        }

        Map<String, BindingOverride> bindings = new LinkedHashMap<>(pack.bindings());
        for (Map.Entry<String, BindingOverride> entry : seed.bindings().entrySet()) {
            if (!bindings.containsKey(entry.getKey())) {
                bindings.put(entry.getKey(), entry.getValue());
                added++;
            }
        }

        // Preserve pack category order, then append any seed-only categories.
        List<CategoryDef> ordered = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CategoryDef category : pack.categories()) {
            CategoryDef resolved = categories.get(category.id());
            if (resolved != null) {
                ordered.add(resolved);
                seen.add(category.id());
            }
        }
        for (CategoryDef category : seed.categories()) {
            if (!seen.contains(category.id())) {
                ordered.add(categories.get(category.id()));
                seen.add(category.id());
            }
        }
        for (CategoryDef category : categories.values()) {
            if (!seen.contains(category.id())) {
                ordered.add(category);
            }
        }

        return new MergeResult(new KeybindProfile(ordered, bindings), added > 0, added);
    }

    private record MergeResult(KeybindProfile profile, boolean dirty, int addedCount) {
    }

    public static KeybindProfile loadMerged() {
        KeybindProfile pack = loadFile(directory().resolve(PACK_DEFAULTS)).orElse(KeybindProfile.EMPTY);
        KeybindProfile player = loadFile(directory().resolve(PLAYER)).orElse(KeybindProfile.EMPTY);
        return merge(pack, player);
    }

    public static void savePlayer(KeybindProfile profile) {
        saveFile(directory().resolve(PLAYER), profile);
    }

    public static java.util.Optional<KeybindProfile> loadFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return java.util.Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            KeybindProfile profile = KeybindProfile.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(message -> new IllegalStateException(message));
            return java.util.Optional.of(profile);
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to load keybind profile {}", path, exception);
            return java.util.Optional.empty();
        }
    }

    public static void saveFile(Path path, KeybindProfile profile) {
        try {
            Files.createDirectories(path.getParent());
            JsonElement json = KeybindProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile)
                    .getOrThrow(message -> new IllegalStateException(message));
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to save keybind profile {}", path, exception);
        }
    }

    /**
     * Player overlay wins on binding keys; categories with the same id are replaced by player versions,
     * and player-only categories are appended.
     */
    public static KeybindProfile merge(KeybindProfile pack, KeybindProfile player) {
        java.util.Map<String, CategoryDef> categories = new java.util.LinkedHashMap<>();
        for (CategoryDef category : pack.categories()) {
            categories.put(category.id(), category);
        }
        for (CategoryDef category : player.categories()) {
            categories.put(category.id(), category);
        }
        java.util.Map<String, BindingOverride> bindings = new java.util.LinkedHashMap<>(pack.bindings());
        bindings.putAll(player.bindings());
        return new KeybindProfile(new java.util.ArrayList<>(categories.values()), bindings);
    }
}
