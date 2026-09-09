package com.ankin.rpgmechanics.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.client.RpgEquipmentScreen;
import com.ankin.rpgmechanics.quest.client.QuestHudOverlay;
import com.ankin.rpgmechanics.quest.client.QuestKeybinds;
import com.ankin.rpgmechanics.registry.ModMenus;
import com.ankin.rpgmechanics.world.border.client.BorderWorldFogRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = RpgMechanics.MOD_ID, dist = Dist.CLIENT)
public class RpgMechanicsClient {
    public RpgMechanicsClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeys);
        modEventBus.addListener(this::onRegisterGuiLayers);
        modEventBus.addListener(this::onRegisterMenuScreens);
        modEventBus.addListener(BorderWorldFogRenderer::registerShaders);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        RpgMechanics.LOGGER.info("RPG Mechanics client setup");
        event.enqueueWork(() -> {
            if (!com.ankin.rpgmechanics.keybind.KeybindManager.isInitialized()) {
                com.ankin.rpgmechanics.keybind.KeybindManager.bootstrap();
            }
        });
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        QuestKeybinds.registerKeys(event);
    }

    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        QuestHudOverlay.register(event);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RPG_EQUIPMENT.get(), RpgEquipmentScreen::new);
    }
}
