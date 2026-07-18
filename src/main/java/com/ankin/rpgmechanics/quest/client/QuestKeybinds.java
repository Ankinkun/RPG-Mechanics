package com.ankin.rpgmechanics.quest.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class QuestKeybinds {
    public static final Lazy<KeyMapping> OPEN_QUEST_BOOK = Lazy.of(() -> new KeyMapping(
            "key.rpgmechanics.open_quest_book",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.rpgmechanics"
    ));

    private QuestKeybinds() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_QUEST_BOOK.get());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_QUEST_BOOK.get().consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            if (minecraft.screen instanceof QuestBookScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new QuestBookScreen());
            }
        }
    }
}
