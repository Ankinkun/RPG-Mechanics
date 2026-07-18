package com.ankin.rpgmechanics.keybind;

import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

/**
 * Gates keybind taxonomy authoring (categories / hide / enable tools).
 * Player rebinds and trigger cycling are always available.
 * <p>
 * Authoring is enabled only when {@code keybinds.keybindAuthoringMode} is true in client config.
 */
public final class KeybindPermissions {
    private KeybindPermissions() {
    }

    public static boolean isAuthoringEnabled() {
        if (!RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            return false;
        }
        try {
            return RpgMechanicsConfig.CLIENT.keybindAuthoringMode.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
