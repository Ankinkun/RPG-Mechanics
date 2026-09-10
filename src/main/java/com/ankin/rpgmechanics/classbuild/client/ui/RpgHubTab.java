package com.ankin.rpgmechanics.classbuild.client.ui;

import net.minecraft.network.chat.Component;

public enum RpgHubTab {
    OVERVIEW("screen.rpgmechanics.hub.overview"),
    GEAR("screen.rpgmechanics.hub.gear"),
    CLASS("screen.rpgmechanics.hub.class"),
    QUESTS("screen.rpgmechanics.hub.quests"),
    MAP("screen.rpgmechanics.hub.map"),
    SKILLS("screen.rpgmechanics.hub.skills");

    private final String langKey;

    RpgHubTab(String langKey) {
        this.langKey = langKey;
    }

    public Component label() {
        return Component.translatable(langKey);
    }
}
