package com.ankin.rpgmechanics.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.mixin.client.KeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Claims physical keys for managed bindings and synthesizes {@link KeyMapping} state.
 */
public final class KeybindInputEngine {
    private static final long DOUBLE_TAP_MS = 300L;
    /** Hold must be sustained this long before one press fires. */
    public static final long HOLD_MS = 500L;

    private static final Map<String, List<Route>> routesByPhysicalKey = new HashMap<>();
    private static final Set<String> managedBindings = new HashSet<>();
    private static final Set<String> claimedPhysicalKeys = new HashSet<>();
    /** binding name → physical chords (for GUI {@code matches} / {@code isActiveAndMatches}). */
    private static final Map<String, List<MatchChord>> matchChordsByBinding = new HashMap<>();

    private static final Map<String, Long> lastTapMs = new HashMap<>();
    private static final Map<String, Boolean> physicalDown = new HashMap<>();
    private static final Set<String> pressHeldBindings = new HashSet<>();
    /** routeKey → hold start time; removed on release or after fire. */
    private static final Map<String, PendingHold> pendingHolds = new HashMap<>();

    private static boolean active;
    private static boolean applying;

    private KeybindInputEngine() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isApplying() {
        return applying;
    }

    public static boolean isManaged(String bindingName) {
        return managedBindings.contains(bindingName);
    }

    public static boolean isPhysicalKeyClaimed(InputConstants.Key key) {
        return key != null && claimedPhysicalKeys.contains(key.getName());
    }

    public static void clear() {
        routesByPhysicalKey.clear();
        managedBindings.clear();
        claimedPhysicalKeys.clear();
        matchChordsByBinding.clear();
        lastTapMs.clear();
        physicalDown.clear();
        pressHeldBindings.clear();
        pendingHolds.clear();
        active = false;
    }

    public static void rebuild(KeybindProfile profile) {
        clear();
        applying = true;
        try {
            KeybindCatalog.refresh();
            for (Map.Entry<String, BindingOverride> entry : profile.bindings().entrySet()) {
                String bindingName = entry.getKey();
                // Own-mod KeyMappings use vanilla consumeClick; managing them unbinds to UNKNOWN (-1)
                // and spams "Invalid key -1". Restore defaults if a prior rebuild already unbound them.
                if (bindingName.startsWith("key.rpgmechanics.")) {
                    KeyMapping own = KeybindCatalog.get(bindingName);
                    if (own != null) {
                        own.setKeyModifierAndCode(KeyModifier.NONE, own.getDefaultKey());
                    }
                    continue;
                }
                BindingOverride override = entry.getValue();
                KeyMapping mapping = KeybindCatalog.get(bindingName);
                if (mapping == null) {
                    continue;
                }
                if (!override.enabled()) {
                    mapping.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                    managedBindings.add(bindingName);
                    continue;
                }
                if (!override.isManaged()) {
                    continue;
                }
                mapping.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                managedBindings.add(bindingName);
                for (KeyChord chord : override.chords()) {
                    chord.resolveKey().ifPresent(physical -> {
                        String physicalName = physical.getName();
                        if (physical.equals(InputConstants.UNKNOWN)) {
                            return;
                        }
                        claimedPhysicalKeys.add(physicalName);
                        routesByPhysicalKey
                                .computeIfAbsent(physicalName, ignored -> new ArrayList<>())
                                .add(new Route(bindingName, chord.modifier(), chord.trigger()));
                        // GUI codepaths (inventory hotbar swap, drop, etc.) use
                        // KeyMapping.isActiveAndMatches / matches against the bound key.
                        // We unbind managed mappings for in-game synthesis, so keep chords here.
                        matchChordsByBinding
                                .computeIfAbsent(bindingName, ignored -> new ArrayList<>())
                                .add(new MatchChord(physical, chord.modifier()));
                    });
                }
            }
            KeyMapping.resetMapping();
            active = !managedBindings.isEmpty();
            RpgMechanics.LOGGER.debug(
                    "Keybind engine rebuilt: {} managed binding(s), {} claimed key(s)",
                    managedBindings.size(),
                    claimedPhysicalKeys.size()
            );
        } finally {
            applying = false;
        }
    }

    /**
     * Advances pending HOLD timers. Call once per client tick.
     */
    public static void tick() {
        if (!active || pendingHolds.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen != null) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, PendingHold>> iterator = pendingHolds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingHold> entry = iterator.next();
            PendingHold pending = entry.getValue();
            if (!physicalDown.getOrDefault(pending.physicalName(), false)) {
                iterator.remove();
                continue;
            }
            if (!modifiersMatch(pending.modifier())) {
                iterator.remove();
                continue;
            }
            if (now - pending.startMs() < HOLD_MS) {
                continue;
            }
            KeyMapping mapping = KeybindCatalog.get(pending.bindingName());
            if (mapping != null) {
                injectClick(mapping);
                // Brief down so consumeClick consumers see a normal edge; clear next tick path via pressHeld.
                pressHeldBindings.add(pending.bindingName());
            }
            iterator.remove();
        }
    }

    /**
     * @return true if vanilla {@link KeyMapping#set} should be cancelled
     */
    public static boolean handleSet(InputConstants.Key key, boolean held) {
        if (!active || applying || key == null) {
            return false;
        }
        if (!isPhysicalKeyClaimed(key)) {
            return false;
        }
        String physicalName = key.getName();
        boolean wasDown = physicalDown.getOrDefault(physicalName, false);
        physicalDown.put(physicalName, held);

        List<Route> routes = routesByPhysicalKey.get(physicalName);
        if (routes == null || routes.isEmpty()) {
            return true;
        }

        // Screens swallow mouse KeyMapping.set; do not synthesize in-game clicks/holds while a GUI is open.
        Minecraft minecraft = Minecraft.getInstance();
        boolean guiOpen = minecraft != null && minecraft.screen != null;

        for (Route route : routes) {
            String routeKey = physicalName + "|" + route.bindingName() + "|" + route.trigger().wireName();
            if (!modifiersMatch(route.modifier())) {
                if (!held && wasDown) {
                    releaseBinding(route.bindingName());
                    pendingHolds.remove(routeKey);
                }
                continue;
            }
            KeyMapping mapping = KeybindCatalog.get(route.bindingName());
            if (mapping == null) {
                continue;
            }
            switch (route.trigger()) {
                case HOLD -> {
                    if (held && !wasDown) {
                        if (guiOpen) {
                            continue;
                        }
                        pendingHolds.put(routeKey, new PendingHold(
                                route.bindingName(),
                                physicalName,
                                route.modifier(),
                                System.currentTimeMillis()
                        ));
                    } else if (!held && wasDown) {
                        pendingHolds.remove(routeKey);
                        setSyntheticDown(mapping, false);
                        pressHeldBindings.remove(route.bindingName());
                    }
                }
                case PRESS -> {
                    if (held && !wasDown) {
                        if (!guiOpen) {
                            injectClick(mapping);
                            setSyntheticDown(mapping, true);
                            pressHeldBindings.add(route.bindingName());
                        }
                    } else if (!held && wasDown) {
                        setSyntheticDown(mapping, false);
                        pressHeldBindings.remove(route.bindingName());
                    }
                }
                case RELEASE -> {
                    if (!held && wasDown && !guiOpen) {
                        injectClick(mapping);
                    }
                }
                case DOUBLE_TAP -> {
                    if (held && !wasDown && !guiOpen) {
                        long now = System.currentTimeMillis();
                        String tapKey = physicalName + "|" + route.bindingName();
                        Long previous = lastTapMs.get(tapKey);
                        if (previous != null && now - previous <= DOUBLE_TAP_MS) {
                            injectClick(mapping);
                            lastTapMs.remove(tapKey);
                        } else {
                            lastTapMs.put(tapKey, now);
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * @return true if vanilla {@link KeyMapping#click} should be cancelled
     */
    public static boolean handleClick(InputConstants.Key key) {
        if (!active || applying || key == null) {
            return false;
        }
        return isPhysicalKeyClaimed(key);
    }

    /**
     * Custom setAll: resync KEYSYM holds from GLFW. Vanilla {@code setAll} never restores mouse
     * mappings — doing so re-holds {@code key.use} after opening a container with right-click.
     *
     * @return true if vanilla setAll should be cancelled
     */
    public static boolean handleSetAll() {
        if (!active) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        long window = minecraft.getWindow().getWindow();
        for (KeyMapping mapping : KeybindCatalog.all()) {
            if (managedBindings.contains(mapping.getName())) {
                continue;
            }
            if (mapping.getKey().getType() == InputConstants.Type.KEYSYM
                    && mapping.getKey().getValue() != InputConstants.UNKNOWN.getValue()) {
                mapping.setDown(InputConstants.isKeyDown(window, mapping.getKey().getValue()));
            }
        }
        pressHeldBindings.clear();
        for (String bindingName : managedBindings) {
            if (resyncManagedKeysymHold(bindingName, window)) {
                pressHeldBindings.add(bindingName);
            }
        }
        return true;
    }

    /**
     * Vanilla calls {@link KeyMapping#releaseAll()} when a screen opens. Mouse release then goes to
     * the GUI, not {@link KeyMapping#set}, so drop mouse hold state or {@code key.use} stays down.
     */
    public static void onReleaseAll() {
        if (!active || applying) {
            return;
        }
        pendingHolds.entrySet().removeIf(entry -> isMousePhysical(entry.getValue().physicalName()));
        pressHeldBindings.removeIf(name -> !hasHeldKeysymChord(name));
        physicalDown.keySet().removeIf(KeybindInputEngine::isMousePhysical);
    }

    /**
     * GUI mouse-up never reaches {@link KeyMapping#set}; keep engine physical-down in sync.
     */
    public static void notifyGuiMouseReleased(int button) {
        if (!active || applying) {
            return;
        }
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        if (!isPhysicalKeyClaimed(key)) {
            return;
        }
        handleSet(key, false);
    }

    /**
     * Whether GUI match queries should use engine chords instead of the unbound {@link KeyMapping} key.
     */
    public static boolean shouldResolveGuiMatch(String bindingName) {
        return active && managedBindings.contains(bindingName);
    }

    /**
     * Match a pressed key against managed chords (primary + secondary).
     *
     * @param requireModifier when true (GUI {@code isActiveAndMatches}), chord modifiers must be active;
     *                        when false ({@code matches}/{@code matchesMouse}), only the physical key is compared
     */
    public static boolean matchesManaged(String bindingName, InputConstants.Key pressed, boolean requireModifier) {
        if (pressed == null || pressed.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        List<MatchChord> chords = matchChordsByBinding.get(bindingName);
        if (chords == null || chords.isEmpty()) {
            return false;
        }
        String pressedName = pressed.getName();
        for (MatchChord chord : chords) {
            if (!chord.physicalKey().getName().equals(pressedName)) {
                continue;
            }
            if (!requireModifier || modifiersMatch(chord.modifier())) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesManaged(String bindingName, InputConstants.Key pressed) {
        return matchesManaged(bindingName, pressed, true);
    }

    public static boolean matchesManagedKeysym(String bindingName, int keysym, int scancode) {
        InputConstants.Key pressed = keysym == InputConstants.UNKNOWN.getValue()
                ? InputConstants.Type.SCANCODE.getOrCreate(scancode)
                : InputConstants.Type.KEYSYM.getOrCreate(keysym);
        return matchesManaged(bindingName, pressed, false);
    }

    public static boolean matchesManagedMouse(String bindingName, int mouseButton) {
        return matchesManaged(bindingName, InputConstants.Type.MOUSE.getOrCreate(mouseButton), false);
    }

    private static boolean resyncManagedKeysymHold(String bindingName, long window) {
        List<MatchChord> chords = matchChordsByBinding.get(bindingName);
        boolean down = false;
        if (chords != null) {
            for (MatchChord chord : chords) {
                InputConstants.Key key = chord.physicalKey();
                if (key.getType() != InputConstants.Type.KEYSYM) {
                    continue;
                }
                if (InputConstants.isKeyDown(window, key.getValue()) && modifiersMatch(chord.modifier())) {
                    down = true;
                    break;
                }
            }
        }
        KeyMapping mapping = KeybindCatalog.get(bindingName);
        if (mapping != null) {
            setSyntheticDown(mapping, down);
        }
        return down;
    }

    private static boolean hasHeldKeysymChord(String bindingName) {
        List<MatchChord> chords = matchChordsByBinding.get(bindingName);
        if (chords == null) {
            return false;
        }
        for (MatchChord chord : chords) {
            InputConstants.Key key = chord.physicalKey();
            if (key.getType() == InputConstants.Type.KEYSYM
                    && physicalDown.getOrDefault(key.getName(), false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMousePhysical(String physicalName) {
        return physicalName != null && physicalName.startsWith("key.mouse.");
    }

    private static void releaseBinding(String bindingName) {
        KeyMapping mapping = KeybindCatalog.get(bindingName);
        if (mapping != null) {
            setSyntheticDown(mapping, false);
        }
        pressHeldBindings.remove(bindingName);
    }

    private static void setSyntheticDown(KeyMapping mapping, boolean down) {
        if (mapping instanceof ToggleKeyMapping && down) {
            return;
        }
        mapping.setDown(down);
    }

    private static void injectClick(KeyMapping mapping) {
        if (mapping instanceof ToggleKeyMapping) {
            mapping.setDown(true);
            return;
        }
        KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
        accessor.rpgmechanics$setClickCount(accessor.rpgmechanics$getClickCount() + 1);
        mapping.setDown(true);
    }

    /**
     * Match NeoForge {@link KeyModifier#NONE}: in-game binds still fire while sneak/sprint modifiers are held.
     */
    private static boolean modifiersMatch(KeyModifier required) {
        if (required == null || required == KeyModifier.NONE) {
            return true;
        }
        return required.isActive(null);
    }

    private record Route(String bindingName, KeyModifier modifier, KeyTriggerMode trigger) {
    }

    private record MatchChord(InputConstants.Key physicalKey, KeyModifier modifier) {
    }

    private record PendingHold(String bindingName, String physicalName, KeyModifier modifier, long startMs) {
    }
}
