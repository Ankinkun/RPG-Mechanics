package com.ankin.rpgmechanics.keybind;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * One physical chord that can fire a logical keybind.
 */
public record KeyChord(
        String key,
        KeyModifier modifier,
        KeyTriggerMode trigger
) {
    private static final Codec<KeyModifier> MODIFIER_CODEC = Codec.STRING.xmap(KeyChord::parseModifier, KeyChord::writeModifier);
    private static final Codec<KeyTriggerMode> TRIGGER_CODEC = Codec.STRING.xmap(KeyTriggerMode::fromString, KeyTriggerMode::wireName);

    public static final Codec<KeyChord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(KeyChord::key),
            MODIFIER_CODEC.optionalFieldOf("modifier", KeyModifier.NONE).forGetter(KeyChord::modifier),
            TRIGGER_CODEC.optionalFieldOf("trigger", KeyTriggerMode.PRESS).forGetter(KeyChord::trigger)
    ).apply(instance, KeyChord::new));

    public KeyChord {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Chord key must not be blank");
        }
        if (modifier == null) {
            modifier = KeyModifier.NONE;
        }
        if (trigger == null) {
            trigger = KeyTriggerMode.PRESS;
        }
    }

    public static KeyChord of(String key, KeyModifier modifier, KeyTriggerMode trigger) {
        return new KeyChord(key, modifier == null ? KeyModifier.NONE : modifier, trigger == null ? KeyTriggerMode.PRESS : trigger);
    }

    private static KeyModifier parseModifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return KeyModifier.NONE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "shift" -> KeyModifier.SHIFT;
            case "control", "ctrl" -> KeyModifier.CONTROL;
            case "alt" -> KeyModifier.ALT;
            default -> KeyModifier.NONE;
        };
    }

    private static String writeModifier(KeyModifier modifier) {
        if (modifier == null || modifier == KeyModifier.NONE) {
            return "none";
        }
        return modifier.name().toLowerCase(Locale.ROOT);
    }

    public Optional<com.mojang.blaze3d.platform.InputConstants.Key> resolveKey() {
        try {
            return Optional.of(com.mojang.blaze3d.platform.InputConstants.getKey(key));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
