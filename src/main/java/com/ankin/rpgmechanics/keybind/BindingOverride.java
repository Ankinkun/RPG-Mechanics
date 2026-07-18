package com.ankin.rpgmechanics.keybind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-binding override stored in the keybind profile JSON.
 */
public record BindingOverride(
        boolean visible,
        boolean enabled,
        Optional<String> customCategory,
        Optional<String> defaultKey,
        List<KeyChord> chords
) {
    public static final Codec<BindingOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(BindingOverride::visible),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(BindingOverride::enabled),
            Codec.STRING.optionalFieldOf("customCategory").forGetter(BindingOverride::customCategory),
            Codec.STRING.optionalFieldOf("defaultKey").forGetter(BindingOverride::defaultKey),
            KeyChord.CODEC.listOf().optionalFieldOf("chords", List.of()).forGetter(BindingOverride::chords)
    ).apply(instance, BindingOverride::new));

    public BindingOverride {
        chords = List.copyOf(chords == null ? List.of() : chords);
    }

    public static BindingOverride defaultsFromVanilla(String vanillaKeyName) {
        List<KeyChord> chords = new ArrayList<>();
        if (vanillaKeyName != null && !vanillaKeyName.isBlank() && !"key.keyboard.unknown".equals(vanillaKeyName)) {
            chords.add(KeyChord.of(vanillaKeyName, net.neoforged.neoforge.client.settings.KeyModifier.NONE, KeyTriggerMode.PRESS));
        }
        return new BindingOverride(true, true, Optional.empty(), Optional.ofNullable(vanillaKeyName), chords);
    }

    public BindingOverride withVisible(boolean value) {
        return new BindingOverride(value, enabled, customCategory, defaultKey, chords);
    }

    public BindingOverride withEnabled(boolean value) {
        return new BindingOverride(visible, value, customCategory, defaultKey, chords);
    }

    public BindingOverride withChords(List<KeyChord> nextChords) {
        return new BindingOverride(visible, enabled, customCategory, defaultKey, nextChords);
    }

    public BindingOverride withCustomCategory(Optional<String> category) {
        return new BindingOverride(visible, enabled, category, defaultKey, chords);
    }

    /**
     * True when this override should take ownership of input (unbind vanilla + synthesize).
     * Empty chords with {@code enabled=true} mean fully unbound (no key assigned).
     */
    public boolean isManaged() {
        return enabled;
    }
}
