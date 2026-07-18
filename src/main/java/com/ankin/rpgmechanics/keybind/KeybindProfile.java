package com.ankin.rpgmechanics.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Full keybind profile: custom categories + per-binding overrides.
 */
public record KeybindProfile(
        List<CategoryDef> categories,
        Map<String, BindingOverride> bindings
) {
    public static final KeybindProfile EMPTY = new KeybindProfile(List.of(), Map.of());

    public static final Codec<KeybindProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CategoryDef.CODEC.listOf().optionalFieldOf("categories", List.of()).forGetter(KeybindProfile::categories),
            Codec.unboundedMap(Codec.STRING, BindingOverride.CODEC).optionalFieldOf("bindings", Map.of()).forGetter(KeybindProfile::bindings)
    ).apply(instance, KeybindProfile::new));

    public KeybindProfile {
        categories = List.copyOf(categories == null ? List.of() : categories);
        bindings = Map.copyOf(bindings == null ? Map.of() : bindings);
    }

    public Optional<BindingOverride> getBinding(String name) {
        return Optional.ofNullable(bindings.get(name));
    }

    public BindingOverride getOrDefault(String name, String vanillaKeyName) {
        return bindings.getOrDefault(name, BindingOverride.defaultsFromVanilla(vanillaKeyName));
    }

    public KeybindProfile withBinding(String name, BindingOverride override) {
        Map<String, BindingOverride> next = new LinkedHashMap<>(bindings);
        next.put(name, override);
        return new KeybindProfile(categories, next);
    }

    public KeybindProfile removeBinding(String name) {
        if (!bindings.containsKey(name)) {
            return this;
        }
        Map<String, BindingOverride> next = new LinkedHashMap<>(bindings);
        next.remove(name);
        return new KeybindProfile(categories, next);
    }

    public KeybindProfile withCategories(List<CategoryDef> nextCategories) {
        return new KeybindProfile(nextCategories, bindings);
    }

    public KeybindProfile upsertCategory(CategoryDef category) {
        List<CategoryDef> next = new ArrayList<>();
        boolean replaced = false;
        for (CategoryDef existing : categories) {
            if (existing.id().equals(category.id())) {
                next.add(category);
                replaced = true;
            } else {
                next.add(existing);
            }
        }
        if (!replaced) {
            next.add(category);
        }
        return withCategories(next);
    }

    public KeybindProfile removeCategory(String categoryId) {
        List<CategoryDef> next = new ArrayList<>();
        for (CategoryDef existing : categories) {
            if (!existing.id().equals(categoryId)) {
                next.add(existing);
            }
        }
        Map<String, BindingOverride> nextBindings = new HashMap<>(bindings);
        for (Map.Entry<String, BindingOverride> entry : bindings.entrySet()) {
            BindingOverride override = entry.getValue();
            if (override.customCategory().isPresent() && override.customCategory().get().equals(categoryId)) {
                nextBindings.put(entry.getKey(), override.withCustomCategory(Optional.empty()));
            }
        }
        return new KeybindProfile(next, nextBindings);
    }

    public Optional<CategoryDef> findCategory(String id) {
        return categories.stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
