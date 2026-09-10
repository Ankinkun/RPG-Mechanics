package com.ankin.rpgmechanics.keybind;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ankin.rpgmechanics.RpgMechanics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.KeyMapping;

/**
 * Versioned pack taxonomy: Movement / Combat / Inventory allowlist + hide everything else.
 */
public final class KeybindTaxonomy {
    private static final String VERSION_FILE = "taxonomy.version";
    private static final String RESOURCE = "/assets/rpgmechanics/keybinds/pack_taxonomy.json";

    private KeybindTaxonomy() {
    }

    public static void applyIfNeeded() {
        Optional<Bundled> bundled = loadBundled();
        if (bundled.isEmpty()) {
            return;
        }
        int applied = readAppliedVersion();
        if (applied >= bundled.get().version()) {
            return;
        }
        apply(bundled.get());
        writeAppliedVersion(bundled.get().version());
        RpgMechanics.LOGGER.info("Applied keybind taxonomy v{}", bundled.get().version());
    }

    private static void apply(Bundled bundled) {
        KeybindCatalog.refresh();
        Path packPath = KeybindProfileIO.directory().resolve("pack_defaults.json");
        KeybindProfile existing = KeybindProfileIO.loadFile(packPath).orElse(KeybindProfileIO.createVanillaRegistrySeed());

        Map<String, BindingOverride> bindings = new LinkedHashMap<>(existing.bindings());
        // Ensure every catalog mapping has an entry
        for (KeyMapping mapping : KeybindCatalog.all()) {
            bindings.putIfAbsent(
                    mapping.getName(),
                    BindingOverride.defaultsFromVanilla(KeybindCatalog.defaultKeyName(mapping))
            );
        }

        Set<String> allow = new HashSet<>(bundled.allowlist());
        Map<String, String> categoryByBinding = new LinkedHashMap<>();
        for (CategoryDef category : bundled.categories()) {
            for (String entry : category.entries()) {
                categoryByBinding.put(entry, category.id());
            }
        }

        for (Map.Entry<String, BindingOverride> entry : new ArrayList<>(bindings.entrySet())) {
            String name = entry.getKey();
            BindingOverride current = entry.getValue();
            boolean allowlisted = allow.contains(name);

            if (allowlisted) {
                String cat = categoryByBinding.getOrDefault(name, current.customCategory().orElse(""));
                BindingOverride next = current.withVisible(true)
                        .withEnabled(true)
                        .withCustomCategory(cat.isEmpty() ? current.customCategory() : Optional.of(cat));

                // Pack defaults we own: inventory = TAB, world map = M, skill GUI = K.
                String forcedDefault = null;
                if ("key.inventory".equals(name)) {
                    forcedDefault = "key.keyboard.tab";
                } else if ("gui.xaero_open_map".equals(name)) {
                    forcedDefault = "key.keyboard.m";
                } else if ("key.epicfight.skill_gui".equals(name)
                        && (next.chords().isEmpty()
                        || next.defaultKey().map(k -> k.isBlank() || "key.keyboard.unknown".equals(k)).orElse(true))) {
                    forcedDefault = "key.keyboard.k";
                }

                if (forcedDefault != null) {
                    next = BindingOverride.defaultsFromVanilla(forcedDefault)
                            .withVisible(true)
                            .withEnabled(true)
                            .withCustomCategory(cat.isEmpty() ? Optional.empty() : Optional.of(cat));
                } else if (next.chords().isEmpty()) {
                    String defaultKey = current.defaultKey()
                            .orElse(KeybindCatalog.get(name) != null
                                    ? KeybindCatalog.defaultKeyName(KeybindCatalog.get(name))
                                    : null);
                    next = BindingOverride.defaultsFromVanilla(defaultKey)
                            .withVisible(true)
                            .withEnabled(true)
                            .withCustomCategory(cat.isEmpty() ? Optional.empty() : Optional.of(cat));
                }
                bindings.put(name, next);
            } else {
                // Hide + unbind everything not on the allowlist (not hide-only).
                bindings.put(name, current.withVisible(false).withEnabled(false).withChords(List.of()));
            }
        }

        KeybindProfile pack = new KeybindProfile(bundled.categories(), bindings);
        KeybindProfileIO.saveFile(packPath, pack);

        // Strip player category list so old mega-category overlays don't resurrect; keep chord edits for allowlist.
        Path playerPath = KeybindProfileIO.directory().resolve("player.json");
        Optional<KeybindProfile> playerOpt = KeybindProfileIO.loadFile(playerPath);
        if (playerOpt.isPresent()) {
            Map<String, BindingOverride> playerBindings = new LinkedHashMap<>();
            for (Map.Entry<String, BindingOverride> entry : playerOpt.get().bindings().entrySet()) {
                if (allow.contains(entry.getKey())) {
                    BindingOverride override = entry.getValue();
                    String cat = categoryByBinding.get(entry.getKey());
                    if (cat != null) {
                        override = override.withCustomCategory(Optional.of(cat)).withVisible(true).withEnabled(true);
                    }
                    if ("key.inventory".equals(entry.getKey())) {
                        override = BindingOverride.defaultsFromVanilla("key.keyboard.tab")
                                .withVisible(true)
                                .withEnabled(true)
                                .withCustomCategory(cat == null ? Optional.empty() : Optional.of(cat));
                    } else if ("gui.xaero_open_map".equals(entry.getKey())) {
                        override = BindingOverride.defaultsFromVanilla("key.keyboard.m")
                                .withVisible(true)
                                .withEnabled(true)
                                .withCustomCategory(cat == null ? Optional.empty() : Optional.of(cat));
                    }
                    playerBindings.put(entry.getKey(), override);
                }
            }
            KeybindProfileIO.saveFile(playerPath, new KeybindProfile(List.of(), playerBindings));
        }
    }

    private static Optional<Bundled> loadBundled() {
        try (InputStream stream = KeybindTaxonomy.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                RpgMechanics.LOGGER.warn("Missing bundled keybind taxonomy {}", RESOURCE);
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int version = root.get("taxonomyVersion").getAsInt();
                List<CategoryDef> categories = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("categories")) {
                    CategoryDef category = CategoryDef.CODEC.parse(JsonOps.INSTANCE, element)
                            .getOrThrow(message -> new IllegalStateException(message));
                    categories.add(category);
                }
                List<String> allowlist = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("allowlist")) {
                    allowlist.add(element.getAsString());
                }
                List<String> prefixes = new ArrayList<>();
                JsonArray prefixArray = root.getAsJsonArray("forceDisablePrefixes");
                if (prefixArray != null) {
                    for (JsonElement element : prefixArray) {
                        prefixes.add(element.getAsString());
                    }
                }
                return Optional.of(new Bundled(version, categories, allowlist, prefixes));
            }
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to load keybind taxonomy", exception);
            return Optional.empty();
        }
    }

    private static int readAppliedVersion() {
        Path path = KeybindProfileIO.directory().resolve(VERSION_FILE);
        if (!Files.isRegularFile(path)) {
            return 0;
        }
        try {
            return Integer.parseInt(Files.readString(path, StandardCharsets.UTF_8).trim());
        } catch (Exception exception) {
            return 0;
        }
    }

    private static void writeAppliedVersion(int version) {
        Path path = KeybindProfileIO.directory().resolve(VERSION_FILE);
        try {
            Files.writeString(path, Integer.toString(version), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to write taxonomy version", exception);
        }
    }

    private record Bundled(
            int version,
            List<CategoryDef> categories,
            List<String> allowlist,
            List<String> forceDisablePrefixes
    ) {
    }
}
