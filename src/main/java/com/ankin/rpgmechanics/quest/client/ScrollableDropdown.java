package com.ankin.rpgmechanics.quest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Button that opens a scrollable option list instead of cycling values.
 */
public class ScrollableDropdown<T> extends AbstractWidget {
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_VISIBLE = 8;

    private final List<T> options = new ArrayList<>();
    private final Function<T, Component> labeler;
    private T value;
    private boolean expanded;
    private int scrollOffset;
    private Runnable onChanged = () -> {
    };

    public ScrollableDropdown(int x, int y, int width, int height, Component message, List<T> options, T initial, Function<T, Component> labeler) {
        super(x, y, width, height, message);
        this.labeler = labeler;
        setOptions(options, initial);
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> {
        } : onChanged;
    }

    public void setOptions(List<T> options, T preferred) {
        this.options.clear();
        this.options.addAll(options);
        if (this.options.isEmpty()) {
            this.value = null;
            return;
        }
        if (preferred != null && this.options.contains(preferred)) {
            this.value = preferred;
        } else if (this.value == null || !this.options.contains(this.value)) {
            this.value = this.options.getFirst();
        }
        this.scrollOffset = 0;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        if (value != null && this.options.contains(value)) {
            this.value = value;
            ensureVisible(this.options.indexOf(value));
        }
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void collapse() {
        this.expanded = false;
    }

    public int popupBottom() {
        return this.getY() + this.height + visibleRows() * ROW_HEIGHT + 2;
    }

    public boolean isMouseOverPopup(double mouseX, double mouseY) {
        if (!this.expanded) {
            return false;
        }
        int left = this.getX();
        int top = this.getY() + this.height;
        int right = left + this.width;
        int bottom = top + visibleRows() * ROW_HEIGHT + 2;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private int visibleRows() {
        return Math.min(MAX_VISIBLE, this.options.size());
    }

    private void ensureVisible(int index) {
        if (index < this.scrollOffset) {
            this.scrollOffset = index;
        } else if (index >= this.scrollOffset + visibleRows()) {
            this.scrollOffset = index - visibleRows() + 1;
        }
        clampScroll();
    }

    private void clampScroll() {
        int max = Math.max(0, this.options.size() - visibleRows());
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, max);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.expanded) {
            this.expanded = true;
            if (this.value != null) {
                ensureVisible(this.options.indexOf(this.value));
            }
            return;
        }
        int relativeY = (int) mouseY - (this.getY() + this.height) - 1;
        if (relativeY < 0) {
            this.expanded = false;
            return;
        }
        int row = relativeY / ROW_HEIGHT;
        int index = this.scrollOffset + row;
        if (index >= 0 && index < this.options.size() && row < visibleRows()) {
            this.value = this.options.get(index);
            this.expanded = false;
            this.onChanged.run();
        } else {
            this.expanded = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) {
            return false;
        }
        if (this.isMouseOver(mouseX, mouseY) || this.isMouseOverPopup(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY);
            return true;
        }
        if (this.expanded) {
            this.expanded = false;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.expanded || (!this.isMouseOver(mouseX, mouseY) && !this.isMouseOverPopup(mouseX, mouseY))) {
            return false;
        }
        if (scrollY > 0) {
            this.scrollOffset--;
        } else if (scrollY < 0) {
            this.scrollOffset++;
        }
        clampScroll();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int bg = this.isHoveredOrFocused() || this.expanded ? 0xFF3A3A3A : 0xFF2A2A2A;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xFF808080);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xFF808080);

        Component label = this.value == null ? this.getMessage() : this.labeler.apply(this.value);
        String text = font.plainSubstrByWidth(label.getString(), this.width - 16);
        graphics.drawString(font, text, this.getX() + 4, this.getY() + (this.height - 8) / 2, 0xFFFFFF, false);
        graphics.drawString(font, this.expanded ? "▲" : "▼", this.getX() + this.width - 10, this.getY() + (this.height - 8) / 2, 0xC0C0C0, false);
    }

    /** Draw the open list above other widgets. Call after {@code super.render}. */
    public void renderPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.expanded || this.options.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int left = this.getX();
        int top = this.getY() + this.height;
        int right = left + this.width;
        int bottom = top + visibleRows() * ROW_HEIGHT + 2;
        // Double-fill so nothing under the popup can show through.
        graphics.fill(left, top, right, bottom, 0xFF000000);
        graphics.fill(left, top, right, bottom, 0xFF1A1A1A);
        graphics.fill(left, top, right, top + 1, 0xFFA0A0A0);
        graphics.fill(left, bottom - 1, right, bottom, 0xFFA0A0A0);
        graphics.fill(left, top, left + 1, bottom, 0xFFA0A0A0);
        graphics.fill(right - 1, top, right, bottom, 0xFFA0A0A0);

        for (int row = 0; row < visibleRows(); row++) {
            int index = this.scrollOffset + row;
            if (index >= this.options.size()) {
                break;
            }
            int rowTop = top + 1 + row * ROW_HEIGHT;
            boolean hover = mouseX >= left && mouseX <= right && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            boolean selected = this.options.get(index).equals(this.value);
            if (hover || selected) {
                graphics.fill(left + 1, rowTop, right - 1, rowTop + ROW_HEIGHT, hover ? 0xFF3A3A8A : 0xFF2E2E2E);
            }
            String text = font.plainSubstrByWidth(this.labeler.apply(this.options.get(index)).getString(), this.width - 8);
            graphics.drawString(font, text, left + 4, rowTop + 2, selected ? 0xFFFFA0 : 0xFFFFFF, false);
        }

        if (this.options.size() > MAX_VISIBLE) {
            graphics.drawString(font, "...", right - 12, bottom - 11, 0x808080, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
