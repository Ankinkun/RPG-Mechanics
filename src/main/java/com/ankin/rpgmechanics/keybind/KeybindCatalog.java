package com.ankin.rpgmechanics.keybind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Snapshot of every registered {@link KeyMapping} (vanilla + mods).
 */
public final class KeybindCatalog {
    private static Map<String, KeyMapping> byName = Map.of();
    private static List<KeyMapping> ordered = List.of();

    private KeybindCatalog() {
    }

    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            byName = Map.of();
            ordered = List.of();
            return;
        }
        Map<String, KeyMapping> next = new LinkedHashMap<>();
        List<KeyMapping> list = new ArrayList<>();
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping == null) {
                continue;
            }
            next.put(mapping.getName(), mapping);
            list.add(mapping);
        }
        list.sort(KeyMapping::compareTo);
        byName = Collections.unmodifiableMap(next);
        ordered = Collections.unmodifiableList(list);
    }

    public static List<KeyMapping> all() {
        return ordered;
    }

    public static Map<String, KeyMapping> byName() {
        return byName;
    }

    @Nullable
    public static KeyMapping get(String name) {
        return byName.get(name);
    }

    public static Optional<KeyMapping> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /**
     * Live binding label from the active language (vanilla + every mod's {@code assets/.../lang}).
     */
    public static Component displayName(KeyMapping mapping) {
        return Component.translatable(mapping.getName());
    }

    public static String displayNameString(KeyMapping mapping) {
        return translateKey(mapping.getName(), mapping.getName());
    }

    /**
     * Live category label. Prefers translating {@code categoryId} from lang files;
     * falls back to a stored pack title (custom categories) or the id itself.
     */
    public static String displayCategoryString(String categoryId, @Nullable String storedTitle) {
        if (categoryId != null && !categoryId.isBlank() && I18n.exists(categoryId)) {
            return I18n.get(categoryId);
        }
        if (storedTitle != null && !storedTitle.isBlank()) {
            return storedTitle;
        }
        return categoryId == null ? "" : categoryId;
    }

    public static String displayCategoryString(CategoryDef category) {
        return displayCategoryString(category.id(), category.title());
    }

    public static String translateKey(String key, String fallback) {
        if (key != null && !key.isBlank() && I18n.exists(key)) {
            return I18n.get(key);
        }
        return fallback == null ? key : fallback;
    }

    public static String currentKeyName(KeyMapping mapping) {
        return mapping.getKey().getName();
    }

    public static String defaultKeyName(KeyMapping mapping) {
        return mapping.getDefaultKey().getName();
    }
}
