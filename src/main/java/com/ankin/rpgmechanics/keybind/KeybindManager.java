package com.ankin.rpgmechanics.keybind;

import java.util.Optional;

import com.ankin.rpgmechanics.RpgMechanics;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Owns the live {@link KeybindProfile} and applies it to catalogued mappings / input engine.
 */
public final class KeybindManager {
    private static KeybindProfile profile = KeybindProfile.EMPTY;
    private static boolean initialized;

    private KeybindManager() {
    }

    public static KeybindProfile profile() {
        return profile;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void bootstrap() {
        warnSoftCompat();
        KeybindCatalog.refresh();
        KeybindProfileIO.ensurePackDefaultsSeeded();
        profile = KeybindProfileIO.loadMerged();
        applyProfile();
        initialized = true;
        RpgMechanics.LOGGER.info(
                "Keybind manager ready: {} mapping(s), {} profile override(s), {} categor(ies)",
                KeybindCatalog.all().size(),
                profile.bindings().size(),
                profile.categories().size()
        );
    }

    public static void reloadFromDisk() {
        KeybindCatalog.refresh();
        KeybindProfileIO.ensurePackDefaultsSeeded();
        profile = KeybindProfileIO.loadMerged();
        applyProfile();
    }

    public static void setProfile(KeybindProfile next) {
        profile = next == null ? KeybindProfile.EMPTY : next;
        KeybindProfileIO.savePlayer(profile);
        applyProfile();
    }

    public static void updateBinding(String name, BindingOverride override) {
        setProfile(profile.withBinding(name, override));
    }

    public static void clearBindingOverride(String name) {
        profile = profile.removeBinding(name);
        KeybindProfileIO.savePlayer(profile);
        KeyMapping mapping = KeybindCatalog.get(name);
        if (mapping != null) {
            mapping.setToDefault();
        }
        // Re-merge so pack_defaults restore the binding when player overlay no longer has it.
        profile = KeybindProfileIO.loadMerged();
        applyProfile();
    }

    /**
     * Restores one binding to its pack/vanilla default key (Reset button).
     */
    public static void resetBinding(KeyMapping mapping) {
        BindingOverride current = effectiveOverride(mapping);
        String defaultKey = current.defaultKey().orElse(KeybindCatalog.defaultKeyName(mapping));
        BindingOverride restored = BindingOverride.defaultsFromVanilla(defaultKey)
                .withCustomCategory(current.customCategory())
                .withVisible(current.visible());
        updateBinding(mapping.getName(), restored);
    }

    public static void toggleVisible(KeyMapping mapping) {
        ensureOverride(mapping);
        BindingOverride current = effectiveOverride(mapping);
        updateBinding(mapping.getName(), current.withVisible(!current.visible()));
    }

    public static void resetAllToDefaults() {
        for (KeyMapping mapping : KeybindCatalog.all()) {
            mapping.setToDefault();
        }
        KeybindProfileIO.savePlayer(KeybindProfile.EMPTY);
        profile = KeybindProfileIO.loadMerged();
        applyProfile();
    }

    public static KeyTriggerMode effectiveTrigger(KeyMapping mapping) {
        BindingOverride override = effectiveOverride(mapping);
        if (!override.chords().isEmpty()) {
            return override.chords().getFirst().trigger();
        }
        return KeyTriggerMode.PRESS;
    }

    public static Optional<KeyChord> primaryChord(KeyMapping mapping) {
        BindingOverride override = effectiveOverride(mapping);
        if (!override.chords().isEmpty()) {
            return Optional.of(override.chords().getFirst());
        }
        if (!mapping.isUnbound() && !isManaged(mapping.getName())) {
            return Optional.of(KeyChord.of(mapping.getKey().getName(), mapping.getKeyModifier(), KeyTriggerMode.PRESS));
        }
        return Optional.empty();
    }

    public static Optional<KeyChord> secondaryChord(KeyMapping mapping) {
        BindingOverride override = effectiveOverride(mapping);
        if (override.chords().size() >= 2) {
            return Optional.of(override.chords().get(1));
        }
        return Optional.empty();
    }

    public static boolean isManaged(String name) {
        return KeybindInputEngine.isManaged(name);
    }

    public static void setChordSlot(KeyMapping mapping, int slot, InputConstants.Key key, KeyModifier modifier) {
        ensureOverride(mapping);
        BindingOverride current = effectiveOverride(mapping);
        KeyTriggerMode trigger = effectiveTrigger(mapping);
        java.util.List<KeyChord> chords = new java.util.ArrayList<>(current.chords());
        while (chords.size() < 2) {
            chords.add(KeyChord.of("key.keyboard.unknown", KeyModifier.NONE, trigger));
        }
        if (key.equals(InputConstants.UNKNOWN)) {
            // Escape: unbind this slot entirely (do not restore pack/vanilla default).
            if (slot == 0) {
                if (chords.size() > 1 && !chords.get(1).key().equals("key.keyboard.unknown")) {
                    chords.set(0, chords.get(1));
                    chords = new java.util.ArrayList<>(chords.subList(0, 1));
                } else {
                    chords = new java.util.ArrayList<>();
                }
            } else {
                chords = chords.isEmpty()
                        ? new java.util.ArrayList<>()
                        : new java.util.ArrayList<>(chords.subList(0, Math.min(1, chords.size())));
                if (!chords.isEmpty() && chords.getFirst().key().equals("key.keyboard.unknown")) {
                    chords = new java.util.ArrayList<>();
                }
            }
        } else {
            chords.set(slot, KeyChord.of(key.getName(), modifier == null ? KeyModifier.NONE : modifier, trigger));
            if (slot == 0 && chords.size() > 1 && chords.get(1).key().equals("key.keyboard.unknown")) {
                chords = new java.util.ArrayList<>(chords.subList(0, 1));
            }
        }
        while (!chords.isEmpty() && chords.getLast().key().equals("key.keyboard.unknown")) {
            chords.removeLast();
        }
        // Empty chords = fully unbound action (managed, no physical key).
        updateBinding(mapping.getName(), current.withChords(chords).withEnabled(true));
    }

    public static void cycleTrigger(KeyMapping mapping) {
        ensureOverride(mapping);
        BindingOverride current = effectiveOverride(mapping);
        KeyTriggerMode next = switch (effectiveTrigger(mapping)) {
            case PRESS -> KeyTriggerMode.HOLD;
            case HOLD -> KeyTriggerMode.DOUBLE_TAP;
            case DOUBLE_TAP -> KeyTriggerMode.RELEASE;
            case RELEASE -> KeyTriggerMode.PRESS;
        };
        java.util.List<KeyChord> chords = new java.util.ArrayList<>();
        if (current.chords().isEmpty()) {
            String keyName = mapping.isUnbound() ? KeybindCatalog.defaultKeyName(mapping) : mapping.getKey().getName();
            chords.add(KeyChord.of(keyName, mapping.getKeyModifier(), next));
        } else {
            for (KeyChord chord : current.chords()) {
                chords.add(KeyChord.of(chord.key(), chord.modifier(), next));
            }
        }
        updateBinding(mapping.getName(), current.withChords(chords).withEnabled(true));
    }

    public static BindingOverride effectiveOverride(KeyMapping mapping) {
        return profile.getOrDefault(mapping.getName(), mapping.getKey().getName());
    }

    public static void ensureOverride(KeyMapping mapping) {
        if (profile.getBinding(mapping.getName()).isPresent()) {
            return;
        }
        BindingOverride seeded = BindingOverride.defaultsFromVanilla(KeybindCatalog.defaultKeyName(mapping));
        // Seed chords from current key if different from default display.
        String current = KeybindCatalog.currentKeyName(mapping);
        if (!mapping.isUnbound()) {
            seeded = seeded.withChords(java.util.List.of(
                    KeyChord.of(current, mapping.getKeyModifier(), KeyTriggerMode.PRESS)
            ));
        }
        profile = profile.withBinding(mapping.getName(), seeded);
        KeybindProfileIO.savePlayer(profile);
        applyProfile();
    }

    public static void applyProfile() {
        KeybindCatalog.refresh();
        KeybindInputEngine.rebuild(profile);
    }

    public static Optional<String> resolveDisplayCategory(KeyMapping mapping) {
        Optional<String> categoryId = resolveCategoryId(mapping);
        if (categoryId.isEmpty()) {
            return Optional.empty();
        }
        String id = categoryId.get();
        Optional<CategoryDef> def = profile.findCategory(id);
        return Optional.of(KeybindCatalog.displayCategoryString(id, def.map(CategoryDef::title).orElse(null)));
    }

    /**
     * Stable category id for a binding (customCategory, profile entries membership, or vanilla category).
     */
    public static Optional<String> resolveCategoryId(KeyMapping mapping) {
        Optional<BindingOverride> override = profile.getBinding(mapping.getName());
        if (override.isPresent() && override.get().customCategory().isPresent()) {
            return override.get().customCategory();
        }
        for (CategoryDef category : profile.categories()) {
            if (category.entries().contains(mapping.getName())) {
                return Optional.of(category.id());
            }
        }
        String vanillaCategory = mapping.getCategory();
        if (vanillaCategory != null && !vanillaCategory.isBlank()) {
            return Optional.of(vanillaCategory);
        }
        return Optional.empty();
    }

    /**
     * Sort index for UI: order of categories in the profile JSON list, then entry order inside that category.
     * Unknown categories / bindings sort after known ones.
     */
    public static int categoryOrderIndex(KeyMapping mapping) {
        Optional<String> categoryId = resolveCategoryId(mapping);
        if (categoryId.isEmpty()) {
            return Integer.MAX_VALUE - 1;
        }
        java.util.List<CategoryDef> categories = profile.categories();
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id().equals(categoryId.get())) {
                return i;
            }
        }
        // Category id known from vanilla/customCategory but not listed in JSON — after listed categories.
        return categories.size();
    }

    public static int bindingOrderIndex(KeyMapping mapping) {
        Optional<String> categoryId = resolveCategoryId(mapping);
        if (categoryId.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        Optional<CategoryDef> def = profile.findCategory(categoryId.get());
        if (def.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int index = def.get().entries().indexOf(mapping.getName());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    public static boolean isVisible(KeyMapping mapping) {
        return profile.getBinding(mapping.getName()).map(BindingOverride::visible).orElse(true);
    }

    public static boolean isEnabled(KeyMapping mapping) {
        return profile.getBinding(mapping.getName()).map(BindingOverride::enabled).orElse(true);
    }

    private static void warnSoftCompat() {
        if (ModList.get().isLoaded("controlling")) {
            RpgMechanics.LOGGER.warn(
                    "Controlling is loaded alongside RPG Mechanics. Controlling fights for the Key Binds screen; "
                            + "remove Controlling for the RPG Mechanics keybind menu. NeoForge also shows this as a discouraged-mod warning on launch."
            );
        }
        if (ModList.get().isLoaded("amecs") || ModList.get().isLoaded("amecsapi")) {
            RpgMechanics.LOGGER.warn("Amecs is loaded; advanced modifiers beyond NeoForge SHIFT/CTRL/ALT may conflict.");
        }
    }

    public static String describeChord(KeyChord chord) {
        InputConstants.Key key = chord.resolveKey().orElse(InputConstants.UNKNOWN);
        String keyLabel = key.getDisplayName().getString();
        if (chord.modifier() != KeyModifier.NONE) {
            return chord.modifier().name() + " + " + keyLabel + " [" + chord.trigger().wireName() + "]";
        }
        return keyLabel + " [" + chord.trigger().wireName() + "]";
    }
}
