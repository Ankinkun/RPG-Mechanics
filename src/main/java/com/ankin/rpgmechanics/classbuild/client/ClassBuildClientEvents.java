package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBarSkillsTick;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.integration.EpicFightClientSoft;
import com.ankin.rpgmechanics.classbuild.integration.XaeroWorldMapClientSoft;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class ClassBuildClientEvents {
    private ClassBuildClientEvents() {
    }

    private static boolean classbuildClientEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildEnabled.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!classbuildClientEnabled()) {
            return;
        }

        // Title → character select (skip when backing out from select itself)
        if (event.getScreen() instanceof TitleScreen
                && !(event.getCurrentScreen() instanceof CharacterSelectScreen)
                && !(event.getCurrentScreen() instanceof ClassSelectScreen)) {
            ClientLocalRoster.load();
            event.setNewScreen(new CharacterSelectScreen(true));
            return;
        }

        if (event.getScreen() instanceof PauseScreen) {
            if (ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
                event.setNewScreen(new RpgOverviewScreen());
            }
            return;
        }

        if (event.getScreen() instanceof InventoryScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.isCreative()) {
                // Creative keeps vanilla inventory / creative menu behavior.
                return;
            }
            if (!ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
                event.setNewScreen(new CharacterSelectScreen(false));
                return;
            }
            event.setCanceled(true);
            minecraft.setScreen(new RpgEquipmentScreen());
        }
    }

    /**
     * Overlay Destiny hub tabs on Xaero's world map so Map sits in the same chrome as other tabs.
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!classbuildClientEnabled() || !ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            return;
        }
        if (!XaeroWorldMapClientSoft.isWorldMapScreen(event.getScreen())) {
            return;
        }
        XaeroWorldMapClientSoft.attachHubTabBar(event.getScreen());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!classbuildClientEnabled() || !ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            return;
        }
        if (!XaeroWorldMapClientSoft.isWorldMapScreen(event.getScreen())) {
            return;
        }
        // Strip under tabs, then re-draw top-row widgets so they stay above the map chrome.
        RpgUiPanels.drawTabBarBg(event.getGuiGraphics(), event.getScreen().width);
        for (var child : event.getScreen().children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                    && widget.getY() < 28) {
                widget.render(
                        event.getGuiGraphics(),
                        event.getMouseX(),
                        event.getMouseY(),
                        event.getPartialTick()
                );
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RpgHubTabBarSkillsTick.tick();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            EpicFightClientSoft.ensureCombatMode();
        }

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!ClassBuildClientPayloadHandlers.hasActiveCharacter()
                && ClassBuildClientPayloadHandlers.pendingSelect()
                && !TitleSelectPending.isFromTitleFlow()) {
            ClassBuildClientPayloadHandlers.clearPendingSelect();
            minecraft.setScreen(new CharacterSelectScreen(false));
        }
    }
}
