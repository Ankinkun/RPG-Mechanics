package com.ankin.rpgmechanics.classbuild.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ankin.rpgmechanics.classbuild.CharacterSlot;
import com.ankin.rpgmechanics.classbuild.GearSlot;
import com.ankin.rpgmechanics.classbuild.StowedInventory;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgGearUi;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;
import com.ankin.rpgmechanics.classbuild.network.DiscardItemsPayload;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Gear hub: Destiny doll + owned-item bag (stowed ∪ equipped outline). Plain Screen — no grey container sheet.
 */
public class RpgEquipmentScreen extends Screen {
    private static final int BAG_ROWS = 6;

    private int dollLeft;
    private int dollTop;
    private int bagLeft;
    private int bagTop;
    private int scrollRow;
    private final Set<Integer> highlightedStowed = new HashSet<>();
    private final Set<GearSlot> highlightedGear = EnumSet.noneOf(GearSlot.class);
    private RpgGearUi.BagCell hoveredCell;
    private RpgGearUi.BagCell holdTarget;
    private long holdStartedAt;
    private boolean fHeld;
    private boolean holdTriggered;
    /** Keep marks when navigating to the destroy confirm dialog. */
    private boolean preserveHighlights;

    public RpgEquipmentScreen() {
        super(Component.translatable("screen.rpgmechanics.hub.gear"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.GEAR);
        layout();
    }

    private void layout() {
        int contentTop = RpgUiTheme.TAB_BAR_H + 20;
        this.dollLeft = this.width / 2 - 220;
        this.dollTop = contentTop + 24;
        this.bagLeft = this.width / 2 + 20;
        this.bagTop = contentTop + 40;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        super.render(graphics, mouseX, mouseY, partialTick);
        layout();

        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);

        int dollCardW = 200;
        int dollCardH = 200;
        RpgUiPanels.drawCard(graphics, this.dollLeft - 12, this.dollTop - 16, dollCardW, dollCardH, true);
        graphics.drawString(this.font, this.title, this.dollLeft, this.dollTop - 12, RpgUiTheme.TEXT, false);

        RpgGearUi.drawDoll(
                graphics,
                this.font,
                character,
                this.minecraft != null ? this.minecraft.player : null,
                this.dollLeft,
                this.dollTop,
                mouseX,
                mouseY
        );

        List<RpgGearUi.BagCell> bag = RpgGearUi.buildBag(character);
        int bagW = RpgGearUi.BAG_COLS * (RpgGearUi.SLOT + 4) + 16;
        int bagH = BAG_ROWS * (RpgGearUi.SLOT + 4) + 28;
        RpgUiPanels.drawCard(graphics, this.bagLeft - 8, this.bagTop - 24, bagW, bagH, false);
        graphics.drawString(
                this.font,
                Component.translatable("screen.rpgmechanics.gear.bag"),
                this.bagLeft,
                this.bagTop - 18,
                RpgUiTheme.TEXT_MUTED,
                false
        );
        RpgGearUi.drawBag(
                graphics,
                this.font,
                bag,
                this.bagLeft,
                this.bagTop,
                BAG_ROWS,
                this.scrollRow,
                this::isHighlighted
        );
        this.hoveredCell = RpgGearUi.hitBag(
                bag,
                this.bagLeft,
                this.bagTop,
                BAG_ROWS,
                this.scrollRow,
                mouseX,
                mouseY
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.gear.hint"),
                this.width / 2,
                this.height - 24,
                RpgUiTheme.TEXT_MUTED
        );
        drawHoldProgress(graphics);

        RpgGearUi.renderHoveredTooltip(
                graphics,
                this.font,
                character,
                this.dollLeft,
                this.dollTop,
                bag,
                this.bagLeft,
                this.bagTop,
                BAG_ROWS,
                this.scrollRow,
                mouseX,
                mouseY
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);
        GearSlot slot = RpgGearUi.hitEquipSlot(this.dollLeft, this.dollTop, mouseX, mouseY);
        if (slot != null) {
            RpgGearUi.handleDollClick(slot, button);
            return true;
        }
        List<RpgGearUi.BagCell> bag = RpgGearUi.buildBag(character);
        RpgGearUi.BagCell cell = RpgGearUi.hitBag(bag, this.bagLeft, this.bagTop, BAG_ROWS, this.scrollRow, mouseX, mouseY);
        if (cell != null) {
            RpgGearUi.handleBagClick(cell, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        CharacterSlot character = ClassBuildClientPayloadHandlers.cachedRoster().activeCharacter()
                .orElse(CharacterSlot.EMPTY);
        int cells = RpgGearUi.buildBag(character).size();
        int maxRow = Math.max(0, (cells + RpgGearUi.BAG_COLS - 1) / RpgGearUi.BAG_COLS - BAG_ROWS);
        if (scrollY > 0) {
            this.scrollRow = Math.max(0, this.scrollRow - 1);
            return true;
        }
        if (scrollY < 0) {
            this.scrollRow = Math.min(maxRow, this.scrollRow + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F) {
            if (!this.fHeld) {
                this.fHeld = true;
                this.holdTriggered = false;
                this.holdStartedAt = Util.getMillis();
                this.holdTarget = this.hoveredCell;
            }
            return true;
        }
        if (keyCode == 256) {
            this.minecraft.setScreen(new RpgOverviewScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F && this.fHeld) {
            this.fHeld = false;
            if (!this.holdTriggered) {
                toggleHighlight(this.holdTarget);
            }
            this.holdTarget = null;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fHeld && !this.holdTriggered && Util.getMillis() - this.holdStartedAt >= 3_000L) {
            this.holdTriggered = true;
            this.fHeld = false;
            requestDiscard();
        }
    }

    private void toggleHighlight(RpgGearUi.BagCell cell) {
        if (cell == null) {
            return;
        }
        if (cell.equipped() && cell.gearSlot() != null) {
            if (!this.highlightedGear.remove(cell.gearSlot()) && selectionSize() < StowedInventory.MAX_DISCARD_BATCH) {
                this.highlightedGear.add(cell.gearSlot());
            }
        } else if (cell.stowedIndex() >= 0) {
            if (!this.highlightedStowed.remove(cell.stowedIndex())
                    && selectionSize() < StowedInventory.MAX_DISCARD_BATCH) {
                this.highlightedStowed.add(cell.stowedIndex());
            }
        }
    }

    private boolean isHighlighted(RpgGearUi.BagCell cell) {
        return cell.equipped()
                ? cell.gearSlot() != null && this.highlightedGear.contains(cell.gearSlot())
                : this.highlightedStowed.contains(cell.stowedIndex());
    }

    private int selectionSize() {
        return this.highlightedStowed.size() + this.highlightedGear.size();
    }

    private void requestDiscard() {
        if (selectionSize() == 0) {
            if (this.holdTarget != null) {
                sendDiscard(List.of(this.holdTarget));
            }
            return;
        }
        List<Integer> stowed = new ArrayList<>(this.highlightedStowed);
        List<Integer> gear = this.highlightedGear.stream().map(GearSlot::ordinal).toList();
        int count = stowed.size() + gear.size();
        this.preserveHighlights = true;
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.preserveHighlights = false;
                    if (confirmed) {
                        PacketDistributor.sendToServer(new DiscardItemsPayload(stowed, gear));
                        clearHighlights();
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("screen.rpgmechanics.gear.destroy_confirm_title"),
                Component.translatable("screen.rpgmechanics.gear.destroy_confirm", count)
        ));
    }

    private void sendDiscard(List<RpgGearUi.BagCell> cells) {
        List<Integer> stowed = cells.stream()
                .filter(cell -> !cell.equipped() && cell.stowedIndex() >= 0)
                .map(RpgGearUi.BagCell::stowedIndex)
                .toList();
        List<Integer> gear = cells.stream()
                .filter(cell -> cell.equipped() && cell.gearSlot() != null)
                .map(cell -> cell.gearSlot().ordinal())
                .toList();
        if (!stowed.isEmpty() || !gear.isEmpty()) {
            PacketDistributor.sendToServer(new DiscardItemsPayload(stowed, gear));
            clearHighlights();
        }
    }

    private void drawHoldProgress(GuiGraphics graphics) {
        if (!this.fHeld) {
            return;
        }
        long elapsed = Util.getMillis() - this.holdStartedAt;
        if (elapsed < 300L) {
            return;
        }
        int width = 120;
        int x = (this.width - width) / 2;
        int y = this.height - 38;
        int fill = Math.min(width, (int) (width * elapsed / 3_000L));
        graphics.fill(x, y, x + width, y + 3, RpgUiTheme.PANEL_EDGE_SOFT);
        graphics.fill(x, y, x + fill, y + 3, RpgUiTheme.ACCENT);
    }

    private void clearHighlights() {
        this.highlightedStowed.clear();
        this.highlightedGear.clear();
    }

    @Override
    public void removed() {
        if (!this.preserveHighlights) {
            clearHighlights();
        }
        this.fHeld = false;
        this.holdTarget = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
