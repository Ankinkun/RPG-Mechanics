package com.ankin.rpgmechanics.classbuild.client.ui;

import java.util.function.Consumer;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.client.CharacterSelectScreen;
import com.ankin.rpgmechanics.classbuild.client.ClassEditScreen;
import com.ankin.rpgmechanics.classbuild.client.RpgEquipmentScreen;
import com.ankin.rpgmechanics.classbuild.client.RpgOverviewScreen;
import com.ankin.rpgmechanics.classbuild.client.TitleSelectPending;
import com.ankin.rpgmechanics.classbuild.integration.EpicFightClientSoft;
import com.ankin.rpgmechanics.classbuild.integration.XaeroWorldMapClientSoft;
import com.ankin.rpgmechanics.quest.client.QuestBookScreen;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * Shared Destiny-style top bar: content tabs left, cog + Quit far right.
 */
public final class RpgHubTabBar {
    private static boolean skillsOpenWarned;
    private static boolean mapOpenWarned;

    private RpgHubTabBar() {
    }

    public static void addTo(Consumer<Button> addWidget, int screenWidth, RpgHubTab active) {
        int y = 4;
        int x = RpgUiTheme.TAB_PAD;
        int tabW = 60;
        for (RpgHubTab tab : RpgHubTab.values()) {
            boolean on = tab == active;
            Button button = Button.builder(tab.label(), b -> open(tab))
                    .bounds(x, y, tabW, 20)
                    .build();
            button.active = !on;
            addWidget.accept(button);
            x += tabW + 4;
        }

        int right = screenWidth - RpgUiTheme.TAB_PAD;
        int quitW = 48;
        int cogW = 24;
        addWidget.accept(Button.builder(Component.translatable("screen.rpgmechanics.hub.quit"), b -> quitToTitle())
                .bounds(right - quitW, y, quitW, 20)
                .build());
        addWidget.accept(Button.builder(Component.literal("\u2699"), b -> openSettings(active))
                .bounds(right - quitW - 4 - cogW, y, cogW, 20)
                .build());
    }

    public static void open(RpgHubTab tab) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (tab) {
            case OVERVIEW -> minecraft.setScreen(new RpgOverviewScreen());
            case GEAR -> {
                if (minecraft.screen instanceof RpgEquipmentScreen) {
                    return;
                }
                minecraft.setScreen(new RpgEquipmentScreen());
            }
            case CLASS -> minecraft.setScreen(new ClassEditScreen());
            case QUESTS -> minecraft.setScreen(new QuestBookScreen());
            case MAP -> openXaeroMap();
            case SKILLS -> openEpicFightSkills();
        }
    }

    public static void openSettings(RpgHubTab returnTab) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen returnTo = switch (returnTab) {
            case OVERVIEW -> new RpgOverviewScreen();
            case GEAR -> new RpgEquipmentScreen();
            case CLASS -> new ClassEditScreen();
            case QUESTS -> new QuestBookScreen();
            case MAP -> new RpgOverviewScreen();
            case SKILLS -> new RpgOverviewScreen();
        };
        minecraft.setScreen(new OptionsScreen(returnTo, minecraft.options));
    }

    /**
     * Destiny leave-session: close menus, disconnect like PauseScreen, land on character select.
     * Does not call {@link Minecraft#stop()}.
     */
    public static void quitToTitle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        minecraft.setScreen(null);

        TitleSelectPending.clear();
        TitleSelectPending.clearSuppress();

        boolean local = minecraft.isLocalServer();
        ServerData serverData = minecraft.getCurrentServer();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        if (local) {
            minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.disconnect();
        }

        minecraft.setScreen(new CharacterSelectScreen(true));
        if (serverData != null && !local) {
            RpgMechanics.LOGGER.debug("Quit from remote server {}", serverData.name);
        }
    }

    private static void openXaeroMap() {
        Minecraft minecraft = Minecraft.getInstance();
        if (XaeroWorldMapClientSoft.openWorldMap()) {
            return;
        }
        if (!ModList.get().isLoaded("xaeroworldmap")) {
            hintMapFailed(minecraft, "Xaero's World Map is not loaded");
            return;
        }
        // Fallback: fire Xaero's open-map key.
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if ("gui.xaero_open_map".equals(mapping.getName())) {
                InputConstants.Key key = mapping.getKey();
                if (key == InputConstants.UNKNOWN) {
                    mapping.setKey(InputConstants.getKey("key.keyboard.m"));
                    KeyMapping.resetMapping();
                    key = mapping.getKey();
                }
                minecraft.setScreen(null);
                if (key != InputConstants.UNKNOWN) {
                    KeyMapping.click(key);
                }
                return;
            }
        }
        hintMapFailed(minecraft, "gui.xaero_open_map binding missing");
    }

    private static void openEpicFightSkills() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(null);
        if (EpicFightClientSoft.openSkillEditScreen()) {
            return;
        }
        if (!ModList.get().isLoaded("epicfight")) {
            hintSkillsFailed(minecraft, "Epic Fight is not loaded");
            return;
        }
        RpgHubTabBarSkillsTick.request();
    }

    static void hintMapFailed(Minecraft minecraft, String detail) {
        if (!mapOpenWarned) {
            mapOpenWarned = true;
            RpgMechanics.LOGGER.warn("Failed to open Xaero World Map: {}", detail);
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("screen.rpgmechanics.hub.map_failed"),
                    true
            );
        }
        minecraft.setScreen(new RpgOverviewScreen());
    }

    static void hintSkillsFailed(Minecraft minecraft, String detail) {
        if (!skillsOpenWarned) {
            skillsOpenWarned = true;
            RpgMechanics.LOGGER.warn("Failed to open Epic Fight skill GUI: {}", detail);
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("screen.rpgmechanics.hub.skills_failed"),
                    true
            );
        }
        minecraft.setScreen(new RpgOverviewScreen());
    }
}
