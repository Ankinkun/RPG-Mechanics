package com.ankin.rpgmechanics;

import org.slf4j.Logger;

import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.ankin.rpgmechanics.quest.network.QuestNetwork;
import com.ankin.rpgmechanics.registry.ModAttachments;
import com.ankin.rpgmechanics.registry.ModCreativeTabs;
import com.ankin.rpgmechanics.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(RpgMechanics.MOD_ID)
public class RpgMechanics {
    public static final String MOD_ID = "rpgmechanics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgMechanics(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(QuestNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, RpgMechanicsConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RpgMechanicsConfig.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("RPG Mechanics common setup");
    }
}
