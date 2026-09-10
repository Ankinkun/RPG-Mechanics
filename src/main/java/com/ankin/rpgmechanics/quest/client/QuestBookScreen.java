package com.ankin.rpgmechanics.quest.client;

import com.ankin.rpgmechanics.quest.PlayerQuestState;
import com.ankin.rpgmechanics.quest.QuestDefinition;
import com.ankin.rpgmechanics.quest.QuestStep;
import com.ankin.rpgmechanics.quest.network.DismissCompletedQuestPayload;
import com.ankin.rpgmechanics.quest.network.ToggleTrackQuestPayload;
import com.ankin.rpgmechanics.classbuild.client.RpgOverviewScreen;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Vanilla-styled quest journal. Left-click opens details; right-click tracks.
 * Completed quests stay until the player clicks Complete to dismiss them from the book
 * (completion history remains on the character).
 */
public class QuestBookScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 220;
    private static final int LIST_WIDTH = 130;

    private QuestList questList;
    private ResourceLocation selectedId;
    private int panelLeft;
    private int panelTop;

    private boolean completeHitActive;
    private int completeHitX;
    private int completeHitY;
    private int completeHitW;
    private int completeHitH;

    public QuestBookScreen() {
        super(Component.translatable("screen.rpgmechanics.quest_book"));
    }

    @Override
    protected void init() {
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = RpgUiTheme.TAB_BAR_H + (this.height - RpgUiTheme.TAB_BAR_H - PANEL_HEIGHT) / 2;

        ResourceLocation previous = this.selectedId;
        this.questList = new QuestList(LIST_WIDTH, PANEL_HEIGHT - 28, this.panelTop + 24);
        this.questList.setX(this.panelLeft + 6);
        this.addRenderableWidget(this.questList);
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.QUESTS);

        if (previous != null) {
            this.selectedId = previous;
            this.questList.selectId(previous);
        }
    }

    /** Called when synced quest state changes while the book is open. */
    public void refreshFromCache() {
        ResourceLocation keep = this.selectedId;
        this.rebuildWidgets();
        if (keep != null && ClientQuestCache.playerState().hasAccepted(keep)) {
            this.selectedId = keep;
            if (this.questList != null) {
                this.questList.selectId(keep);
            }
        } else {
            this.selectedId = null;
        }
    }

    private void dismissSelected() {
        if (this.selectedId == null) {
            return;
        }
        PacketDistributor.sendToServer(new DismissCompletedQuestPayload(this.selectedId));
    }

    private boolean isOverCompleteHit(double mouseX, double mouseY) {
        return this.completeHitActive
                && mouseX >= this.completeHitX
                && mouseX < this.completeHitX + this.completeHitW
                && mouseY >= this.completeHitY
                && mouseY < this.completeHitY + this.completeHitH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverCompleteHit(mouseX, mouseY)) {
            dismissSelected();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.completeHitActive = false;
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, RpgUiTheme.TEXT);

        int detailLeft = this.panelLeft + LIST_WIDTH + 14;
        int detailWidth = PANEL_WIDTH - LIST_WIDTH - 24;
        int detailTop = this.panelTop + 24;
        int detailBottom = this.panelTop + PANEL_HEIGHT - 6;
        graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, 0x88000000);
        graphics.fill(detailLeft, detailTop, detailLeft + detailWidth, detailBottom, 0xAA000000);

        graphics.drawString(
                this.font,
                Component.translatable("screen.rpgmechanics.book_hint"),
                this.panelLeft + 6,
                this.panelTop + PANEL_HEIGHT + 4,
                0x808080,
                false
        );

        renderDetails(graphics, mouseX, mouseY, detailLeft, detailWidth, detailTop, detailBottom);
    }

    private void renderDetails(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int detailLeft,
            int detailWidth,
            int detailTop,
            int detailBottom
    ) {
        if (ClientQuestCache.playerState().accepted().isEmpty()) {
            int y = detailTop + 8;
            for (FormattedCharSequence line : this.font.split(
                    Component.translatable("screen.rpgmechanics.book_empty"),
                    detailWidth - 16
            )) {
                graphics.drawString(this.font, line, detailLeft + 8, y, 0xA0A0A0, false);
                y += 10;
            }
            return;
        }

        if (this.selectedId == null) {
            int y = detailTop + 8;
            for (FormattedCharSequence line : this.font.split(
                    Component.translatable("screen.rpgmechanics.select_quest"),
                    detailWidth - 16
            )) {
                graphics.drawString(this.font, line, detailLeft + 8, y, 0xA0A0A0, false);
                y += 10;
            }
            return;
        }

        QuestDefinition quest = ClientQuestCache.get(this.selectedId).orElse(null);
        PlayerQuestState state = ClientQuestCache.playerState();
        if (quest == null) {
            graphics.drawString(this.font, Component.literal(this.selectedId.toString()), detailLeft + 8, detailTop + 8, 0xFF5555, false);
            return;
        }

        int y = detailTop + 8;
        if (quest.icon().isPresent()) {
            graphics.renderFakeItem(QuestIcons.resolveItemStack(quest.icon()), detailLeft + 8, y);
            graphics.drawString(this.font, quest.title(), detailLeft + 28, y + 4, 0xFFFFFF, false);
            y += 20;
        } else {
            graphics.drawString(this.font, quest.title(), detailLeft + 8, y, 0xFFFFFF, false);
            y += 12;
        }
        if (state.isTracked(quest.id())) {
            graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.tracked"), detailLeft + 8, y, 0x55FF55, false);
            y += 12;
        }
        for (FormattedCharSequence line : this.font.split(Component.literal(quest.description()), detailWidth - 16)) {
            graphics.drawString(this.font, line, detailLeft + 8, y, 0xCFCFCF, false);
            y += 10;
            if (y > detailBottom - 40) {
                break;
            }
        }
        y += 8;

        int current = state.stepIndex(quest.id());
        QuestStep step = quest.stepAt(current);
        if (step == null) {
            Component label = Component.translatable("screen.rpgmechanics.quest_complete");
            this.completeHitX = detailLeft + 8;
            this.completeHitY = y;
            this.completeHitW = Math.max(this.font.width(label) + 8, 48);
            this.completeHitH = 12;
            this.completeHitActive = true;
            boolean hovered = isOverCompleteHit(mouseX, mouseY);
            int color = hovered ? 0xFFFFFF : 0x55FF55;
            graphics.drawString(this.font, label, this.completeHitX, this.completeHitY, color, false);
            return;
        }

        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.current_objective"), detailLeft + 8, y, 0xFFFF55, false);
        y += 12;
        graphics.drawString(this.font, step.title(), detailLeft + 8, y, 0xFFFFFF, false);
        y += 12;
        if (step.description() != null && !step.description().isBlank()) {
            for (FormattedCharSequence line : this.font.split(Component.literal(step.description()), detailWidth - 16)) {
                graphics.drawString(this.font, line, detailLeft + 8, y, 0xA0A0A0, false);
                y += 10;
                if (y > detailBottom - 12) {
                    break;
                }
            }
        }
    }

    private void select(ResourceLocation id) {
        this.selectedId = id;
    }

    private class QuestList extends ObjectSelectionList<QuestList.Entry> {
        public QuestList(int width, int height, int y) {
            super(QuestBookScreen.this.minecraft, width, height, y, 18);
            PlayerQuestState state = ClientQuestCache.playerState();
            for (ResourceLocation id : state.accepted()) {
                ClientQuestCache.get(id).ifPresentOrElse(
                        quest -> this.addEntry(new Entry(quest)),
                        () -> this.addEntry(new Entry(id))
                );
            }
        }

        private void selectId(ResourceLocation id) {
            for (Entry entry : this.children()) {
                if (entry.id.equals(id)) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && this.isMouseOver(mouseX, mouseY)) {
                Entry entry = this.getEntryAtPosition(mouseX, mouseY);
                if (entry != null) {
                    QuestBookScreen.this.select(entry.id);
                    this.setSelected(entry);
                    PacketDistributor.sendToServer(new ToggleTrackQuestPayload(entry.id));
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ResourceLocation id;
            private final String label;

            private Entry(QuestDefinition quest) {
                this.id = quest.id();
                this.label = quest.title();
            }

            private Entry(ResourceLocation missingId) {
                this.id = missingId;
                this.label = missingId.toString();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                PlayerQuestState state = ClientQuestCache.playerState();
                int color = state.isTracked(this.id) ? 0x55FF55 : (hovering ? 0xFFFFA0 : 0xFFFFFF);
                String text = state.isTracked(this.id) ? "★ " + this.label : this.label;
                graphics.drawString(QuestBookScreen.this.font, text, left + 2, top + 5, color, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                QuestBookScreen.this.select(this.id);
                QuestList.this.setSelected(this);
                if (button == 1) {
                    PacketDistributor.sendToServer(new ToggleTrackQuestPayload(this.id));
                }
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.label);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(new RpgOverviewScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
