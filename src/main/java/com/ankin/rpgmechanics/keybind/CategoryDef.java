package com.ankin.rpgmechanics.keybind;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Pack/player-defined category that can group bindings across mods.
 */
public record CategoryDef(
        String id,
        String title,
        List<String> entries
) {
    public static final Codec<CategoryDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CategoryDef::id),
            Codec.STRING.fieldOf("title").forGetter(CategoryDef::title),
            Codec.STRING.listOf().optionalFieldOf("entries", List.of()).forGetter(CategoryDef::entries)
    ).apply(instance, CategoryDef::new));

    public CategoryDef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category id required");
        }
        title = title == null || title.isBlank() ? id : title;
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public CategoryDef withEntries(List<String> next) {
        return new CategoryDef(id, title, next);
    }

    public CategoryDef withTitle(String nextTitle) {
        return new CategoryDef(id, nextTitle, entries);
    }

    public CategoryDef addEntry(String bindingName) {
        if (entries.contains(bindingName)) {
            return this;
        }
        List<String> next = new ArrayList<>(entries);
        next.add(bindingName);
        return withEntries(next);
    }

    public CategoryDef removeEntry(String bindingName) {
        if (!entries.contains(bindingName)) {
            return this;
        }
        List<String> next = new ArrayList<>(entries);
        next.remove(bindingName);
        return withEntries(next);
    }
}
