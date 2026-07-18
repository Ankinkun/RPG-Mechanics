package com.ankin.rpgmechanics.quest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Scrollable grid of large item/custom icons for quest icon picking.
 */
public class IconGridBrowser extends AbstractWidget {
    private static final int CELL = 36;
    private static final int PAD = 4;
    private static final int ICON = 24;

    private final List<ResourceLocation> allIcons = new ArrayList<>();
    private final List<ResourceLocation> filtered = new ArrayList<>();
    private final Consumer<ResourceLocation> onSelect;
    private int scrollRows;
    private int hoveredIndex = -1;

    public IconGridBrowser(int x, int y, int width, int height, Consumer<ResourceLocation> onSelect) {
        super(x, y, width, height, Component.empty());
        this.onSelect = onSelect;
        reload("");
    }

    public void reload(String filter) {
        this.allIcons.clear();
        this.allIcons.addAll(QuestIcons.listCustomIcons());
        this.allIcons.addAll(QuestIcons.listItemIcons());
        this.filtered.clear();
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        for (ResourceLocation id : this.allIcons) {
            if (needle.isEmpty() || id.toString().contains(needle) || id.getPath().contains(needle)) {
                this.filtered.add(id);
            }
        }
        this.scrollRows = 0;
        clampScroll();
    }

    private int columns() {
        return Math.max(1, (this.width - PAD) / (CELL + PAD));
    }

    private int visibleRows() {
        return Math.max(1, (this.height - PAD) / (CELL + PAD));
    }

    private int totalRows() {
        return (this.filtered.size() + columns() - 1) / columns();
    }

    private void clampScroll() {
        int max = Math.max(0, totalRows() - visibleRows());
        this.scrollRows = Mth.clamp(this.scrollRows, 0, max);
    }

    private int indexAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return -1;
        }
        int col = (int) ((mouseX - this.getX() - PAD) / (CELL + PAD));
        int row = (int) ((mouseY - this.getY() - PAD) / (CELL + PAD));
        if (col < 0 || col >= columns() || row < 0 || row >= visibleRows()) {
            return -1;
        }
        int index = (this.scrollRows + row) * columns() + col;
        return index >= 0 && index < this.filtered.size() ? index : -1;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int index = indexAt(mouseX, mouseY);
        if (index >= 0) {
            this.onSelect.accept(this.filtered.get(index));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (scrollY > 0) {
            this.scrollRows--;
        } else if (scrollY < 0) {
            this.scrollRows++;
        }
        clampScroll();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF101010);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xFF808080);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xFF808080);
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, 0xFF808080);
        graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF808080);

        this.hoveredIndex = indexAt(mouseX, mouseY);
        int cols = columns();
        int start = this.scrollRows * cols;
        int end = Math.min(this.filtered.size(), start + visibleRows() * cols);

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % cols;
            int row = local / cols;
            int cellX = this.getX() + PAD + col * (CELL + PAD);
            int cellY = this.getY() + PAD + row * (CELL + PAD);

            boolean hover = i == this.hoveredIndex;
            // Cell backdrop + soft "shadow" rim so icons read as separate tiles.
            graphics.fill(cellX + 1, cellY + 1, cellX + CELL + 1, cellY + CELL + 1, 0x66000000);
            graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, hover ? 0xFF3A3A55 : 0xFF2A2A2A);
            graphics.fill(cellX, cellY, cellX + CELL, cellY + 1, 0xFF555555);
            graphics.fill(cellX, cellY + CELL - 1, cellX + CELL, cellY + CELL, 0xFF555555);

            ResourceLocation icon = this.filtered.get(i);
            int iconX = cellX + (CELL - ICON) / 2;
            int iconY = cellY + (CELL - ICON) / 2;
            if (icon.getNamespace().equals("rpgmechanics") && icon.getPath().startsWith("quest_icon/")) {
                QuestIcons.ensureCustomIconsLoaded();
                graphics.blit(icon, iconX, iconY, 0, 0, ICON, ICON, ICON, ICON);
            } else {
                graphics.renderFakeItem(QuestIcons.resolveItemStack(icon), iconX, iconY);
            }
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.filtered.size()) {
            Minecraft minecraft = Minecraft.getInstance();
            String tip = this.filtered.get(this.hoveredIndex).toString();
            graphics.renderTooltip(minecraft.font, Component.literal(tip), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
