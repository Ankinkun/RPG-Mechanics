package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.CharacterRoster;
import com.ankin.rpgmechanics.classbuild.CharacterSlot;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;
import com.ankin.rpgmechanics.classbuild.network.DeleteCharacterPayload;
import com.ankin.rpgmechanics.classbuild.network.SelectCharacterPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Destiny-style character cards (3 slots). Title flow loads campaign world; in-world uses packets.
 */
public class CharacterSelectScreen extends Screen {
    private final boolean titleFlow;

    public CharacterSelectScreen() {
        this(false);
    }

    public CharacterSelectScreen(boolean titleFlow) {
        super(Component.translatable("screen.rpgmechanics.character_select"));
        this.titleFlow = titleFlow;
    }

    public void refresh() {
        this.rebuildWidgets();
    }

    private CharacterRoster roster() {
        return titleFlow ? ClientLocalRoster.get() : ClassBuildClientPayloadHandlers.cachedRoster();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        CharacterRoster roster = roster();
        int cardWidth = 160;
        int gap = 20;
        int total = CharacterRoster.SLOT_COUNT * cardWidth + (CharacterRoster.SLOT_COUNT - 1) * gap;
        int startX = (this.width - total) / 2;
        int cardY = this.height / 2 - 80;

        for (int i = 0; i < CharacterRoster.SLOT_COUNT; i++) {
            int slot = i;
            int x = startX + i * (cardWidth + gap);
            CharacterSlot character = roster.slot(i);
            if (!character.occupied()) {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.rpgmechanics.character_empty"),
                        b -> this.minecraft.setScreen(new ClassSelectScreen(slot, titleFlow))
                ).bounds(x, cardY + 100, cardWidth, 22).build());
            } else {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.rpgmechanics.character_play"),
                        b -> play(slot)
                ).bounds(x, cardY + 100, cardWidth, 22).build());
                this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.rpgmechanics.character_delete"),
                        b -> delete(slot)
                ).bounds(x, cardY + 126, cardWidth, 22).build());
            }
        }

        if (titleFlow) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.back"),
                    b -> this.minecraft.setScreen(new TitleScreen())
            ).bounds(12, this.height - 28, 80, 20).build());
        }
    }

    private void play(int slot) {
        if (titleFlow) {
            TitleSelectPending.playExisting(slot);
        } else {
            PacketDistributor.sendToServer(new SelectCharacterPayload(slot));
            this.onClose();
        }
    }

    private void delete(int slot) {
        if (titleFlow) {
            ClientLocalRoster.deleteSlot(slot);
            rebuildWidgets();
        } else {
            PacketDistributor.sendToServer(new DeleteCharacterPayload(slot));
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 28, RpgUiTheme.TEXT);

        CharacterRoster roster = roster();
        int cardWidth = 160;
        int gap = 20;
        int total = CharacterRoster.SLOT_COUNT * cardWidth + (CharacterRoster.SLOT_COUNT - 1) * gap;
        int startX = (this.width - total) / 2;
        int cardY = this.height / 2 - 80;

        for (int i = 0; i < CharacterRoster.SLOT_COUNT; i++) {
            int x = startX + i * (cardWidth + gap);
            CharacterSlot character = roster.slot(i);
            RpgUiPanels.drawCard(graphics, x, cardY, cardWidth, 88, character.occupied());

            if (!character.occupied()) {
                graphics.drawCenteredString(
                        this.font,
                        Component.translatable("screen.rpgmechanics.character_new"),
                        x + cardWidth / 2,
                        cardY + 36,
                        RpgUiTheme.TEXT_MUTED
                );
            } else {
                graphics.drawCenteredString(this.font, character.name(), x + cardWidth / 2, cardY + 18, RpgUiTheme.TEXT);
                graphics.drawCenteredString(
                        this.font,
                        Component.translatable("screen.rpgmechanics.darkness_dd"),
                        x + cardWidth / 2,
                        cardY + 40,
                        RpgUiTheme.ACCENT
                );
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !titleFlow && ClassBuildClientPayloadHandlers.hasActiveCharacter();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
