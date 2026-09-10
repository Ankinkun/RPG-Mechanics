package com.ankin.rpgmechanics.classbuild.client;

import org.lwjgl.glfw.GLFW;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.network.CastAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CombatHudKeybinds {
    public static final Lazy<KeyMapping> ABILITY_1 = Lazy.of(() -> new KeyMapping(
            "key.rpgmechanics.ability_1",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Q,
            "key.categories.rpgmechanics"
    ));
    public static final Lazy<KeyMapping> ABILITY_2 = Lazy.of(() -> new KeyMapping(
            "key.rpgmechanics.ability_2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            "key.categories.rpgmechanics"
    ));
    public static final Lazy<KeyMapping> ABILITY_3 = Lazy.of(() -> new KeyMapping(
            "key.rpgmechanics.ability_3",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.rpgmechanics"
    ));
    public static final Lazy<KeyMapping> ABILITY_4 = Lazy.of(() -> new KeyMapping(
            "key.rpgmechanics.ability_4",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.rpgmechanics"
    ));

    private CombatHudKeybinds() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_1.get());
        event.register(ABILITY_2.get());
        event.register(ABILITY_3.get());
        event.register(ABILITY_4.get());
        RpgMechanics.LOGGER.debug("Registered combat ability keybinds");
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            return;
        }
        while (ABILITY_1.get().consumeClick()) {
            PacketDistributor.sendToServer(new CastAbilityPayload(0));
        }
        while (ABILITY_2.get().consumeClick()) {
            PacketDistributor.sendToServer(new CastAbilityPayload(1));
        }
        while (ABILITY_3.get().consumeClick()) {
            PacketDistributor.sendToServer(new CastAbilityPayload(2));
        }
        while (ABILITY_4.get().consumeClick()) {
            PacketDistributor.sendToServer(new CastAbilityPayload(3));
        }
    }

    public static String hint(int index) {
        KeyMapping mapping = switch (index) {
            case 0 -> ABILITY_1.get();
            case 1 -> ABILITY_2.get();
            case 2 -> ABILITY_3.get();
            case 3 -> ABILITY_4.get();
            default -> null;
        };
        if (mapping == null) {
            return "";
        }
        return mapping.getTranslatedKeyMessage().getString();
    }
}
