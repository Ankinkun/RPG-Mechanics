package com.ankin.rpgmechanics.keybind;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.keybind.client.RpgKeybindsScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class KeybindClientEvents {
    private KeybindClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!KeybindManager.isInitialized()) {
            KeybindManager.bootstrap();
        }
        KeybindInputEngine.tick();
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof KeyBindsScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            event.setNewScreen(new RpgKeybindsScreen(event.getCurrentScreen(), minecraft.options));
        }
    }

    /**
     * Container GUIs consume the mouse-up from the right-click that opened them, so vanilla never
     * calls {@code KeyMapping.set(mouse, false)}. Drop our synthetic use-hold on that release.
     */
    @SubscribeEvent
    public static void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        KeybindInputEngine.notifyGuiMouseReleased(event.getButton());
    }
}
