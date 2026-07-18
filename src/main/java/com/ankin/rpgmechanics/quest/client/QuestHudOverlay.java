package com.ankin.rpgmechanics.quest.client;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.PlayerQuestState;
import com.ankin.rpgmechanics.quest.QuestDefinition;
import com.ankin.rpgmechanics.quest.QuestStep;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class QuestHudOverlay {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "quest_hud");
    private static final int MAX_OBJECTIVE_WIDTH = 180;
    private static final int MAX_OBJECTIVE_LINES = 3;
    private static final int ICON_SIZE = 16;

    private QuestHudOverlay() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, LAYER_ID, QuestHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        PlayerQuestState state = ClientQuestCache.playerState();
        if (state.tracked().isEmpty()) {
            return;
        }

        QuestDefinition quest = ClientQuestCache.get(state.tracked().get()).orElse(null);
        if (quest == null) {
            return;
        }

        int x = 8;
        int y = 8;
        int textX = x;

        if (quest.icon().isPresent()) {
            ResourceLocation icon = quest.icon().get();
            var texture = QuestIcons.resolveTexture(icon);
            if (texture.isPresent() && icon.getPath().startsWith("quest_icon/")) {
                QuestIcons.ensureCustomIconsLoaded();
                graphics.blit(texture.get(), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } else {
                graphics.renderFakeItem(QuestIcons.resolveItemStack(icon), x, y);
            }
            textX = x + ICON_SIZE + 4;
        }

        graphics.drawString(minecraft.font, Component.literal(quest.title()), textX, y + 4, 0xFFFFFF, true);
        y += Math.max(ICON_SIZE, 10) + 2;

        QuestStep step = quest.stepAt(state.stepIndex(quest.id()));
        if (step == null) {
            graphics.drawString(minecraft.font, Component.translatable("screen.rpgmechanics.quest_complete"), x, y, 0x55FF55, true);
            return;
        }

        String objective = step.description() != null && !step.description().isBlank()
                ? step.description()
                : step.title();
        int lines = 0;
        for (FormattedCharSequence line : minecraft.font.split(Component.literal(objective), MAX_OBJECTIVE_WIDTH)) {
            graphics.drawString(minecraft.font, line, x, y, 0xE0E0E0, true);
            y += 10;
            lines++;
            if (lines >= MAX_OBJECTIVE_LINES) {
                break;
            }
        }
    }
}
