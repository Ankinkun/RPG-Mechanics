package com.ankin.rpgmechanics.keybind;

/**
 * How a chord activates the underlying {@link net.minecraft.client.KeyMapping}.
 */
public enum KeyTriggerMode {
    PRESS,
    RELEASE,
    HOLD,
    DOUBLE_TAP;

    public static KeyTriggerMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return PRESS;
        }
        return switch (raw.trim().toLowerCase()) {
            case "release" -> RELEASE;
            case "hold" -> HOLD;
            case "double_tap", "doubletap", "double-tap" -> DOUBLE_TAP;
            default -> PRESS;
        };
    }

    public String wireName() {
        return switch (this) {
            case PRESS -> "press";
            case RELEASE -> "release";
            case HOLD -> "hold";
            case DOUBLE_TAP -> "double_tap";
        };
    }
}
