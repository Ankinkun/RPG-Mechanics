package com.ankin.rpgmechanics.classbuild.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.ankin.rpgmechanics.classbuild.CharacterSlot;
import com.ankin.rpgmechanics.classbuild.GearSlot;
import com.ankin.rpgmechanics.classbuild.network.EquipStowedPayload;
import com.ankin.rpgmechanics.classbuild.network.UnequipSlotPayload;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shared Destiny doll (6 slots) + optional owned-item bag grid.
 */
public final class RpgGearUi {
    public static final int SLOT = 22;
    public static final int BAG_COLS = 6;

    private RpgGearUi() {
    }

    public static void drawDoll(
            GuiGraphics graphics,
            Font font,
            CharacterSlot character,
            LivingEntity entity,
            int dollLeft,
            int dollTop,
            int mouseX,
            int mouseY
    ) {
        int modelX0 = dollLeft + 36;
        int modelY0 = dollTop + 8;
        int modelX1 = dollLeft + 140;
        int modelY1 = dollTop + 168;
        if (entity != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    modelX0,
                    modelY0,
                    modelX1,
                    modelY1,
                    42,
                    0.0625F,
                    mouseX,
                    mouseY,
                    entity
            );
        }

        // Left column: armor
        drawEquipSlot(graphics, font, character, GearSlot.HELMET, dollLeft + 8, dollTop + 16);
        drawEquipSlot(graphics, font, character, GearSlot.CHEST, dollLeft + 8, dollTop + 44);
        drawEquipSlot(graphics, font, character, GearSlot.LEGS, dollLeft + 8, dollTop + 72);
        drawEquipSlot(graphics, font, character, GearSlot.BOOTS, dollLeft + 8, dollTop + 100);
        // Right: weapon + offhand
        drawEquipSlot(graphics, font, character, GearSlot.WEAPON, dollLeft + 148, dollTop + 44);
        drawEquipSlot(graphics, font, character, GearSlot.OFFHAND, dollLeft + 148, dollTop + 72);
    }

    public static void drawEquipSlot(GuiGraphics graphics, Font font, CharacterSlot character, GearSlot slot, int x, int y) {
        ItemStack stack = character.get(slot);
        graphics.fill(x, y, x + SLOT, y + SLOT, 0xFF1A1C24);
        graphics.renderOutline(x, y, SLOT, SLOT, RpgUiTheme.PANEL_EDGE);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 3, y + 3);
            graphics.renderItemDecorations(font, stack, x + 3, y + 3);
        }
    }

    public static GearSlot hitEquipSlot(int dollLeft, int dollTop, double mouseX, double mouseY) {
        if (hit(dollLeft + 8, dollTop + 16, mouseX, mouseY)) {
            return GearSlot.HELMET;
        }
        if (hit(dollLeft + 8, dollTop + 44, mouseX, mouseY)) {
            return GearSlot.CHEST;
        }
        if (hit(dollLeft + 8, dollTop + 72, mouseX, mouseY)) {
            return GearSlot.LEGS;
        }
        if (hit(dollLeft + 8, dollTop + 100, mouseX, mouseY)) {
            return GearSlot.BOOTS;
        }
        if (hit(dollLeft + 148, dollTop + 44, mouseX, mouseY)) {
            return GearSlot.WEAPON;
        }
        if (hit(dollLeft + 148, dollTop + 72, mouseX, mouseY)) {
            return GearSlot.OFFHAND;
        }
        return null;
    }

    public static List<BagCell> buildBag(CharacterSlot character) {
        List<BagCell> cells = new ArrayList<>();
        for (GearSlot slot : GearSlot.values()) {
            ItemStack stack = character.get(slot);
            if (!stack.isEmpty()) {
                cells.add(new BagCell(stack, true, slot, -1));
            }
        }
        List<ItemStack> stowed = character.stowed();
        for (int i = 0; i < stowed.size(); i++) {
            ItemStack stack = stowed.get(i);
            if (!stack.isEmpty()) {
                cells.add(new BagCell(stack, false, null, i));
            }
        }
        return cells;
    }

    public static void drawBag(
            GuiGraphics graphics,
            Font font,
            List<BagCell> cells,
            int bagLeft,
            int bagTop,
            int rowsVisible,
            int scrollRow
    ) {
        int max = Math.min(cells.size(), (scrollRow + rowsVisible) * BAG_COLS);
        for (int index = scrollRow * BAG_COLS; index < max; index++) {
            int local = index - scrollRow * BAG_COLS;
            int col = local % BAG_COLS;
            int row = local / BAG_COLS;
            int x = bagLeft + col * (SLOT + 4);
            int y = bagTop + row * (SLOT + 4);
            BagCell cell = cells.get(index);
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xFF1A1C24);
            graphics.renderOutline(x, y, SLOT, SLOT, cell.equipped() ? RpgUiTheme.ACCENT : RpgUiTheme.PANEL_EDGE);
            graphics.renderItem(cell.stack(), x + 3, y + 3);
            graphics.renderItemDecorations(font, cell.stack(), x + 3, y + 3);
        }
    }

    public static BagCell hitBag(
            List<BagCell> cells,
            int bagLeft,
            int bagTop,
            int rowsVisible,
            int scrollRow,
            double mouseX,
            double mouseY
    ) {
        int max = Math.min(cells.size(), (scrollRow + rowsVisible) * BAG_COLS);
        for (int index = scrollRow * BAG_COLS; index < max; index++) {
            int local = index - scrollRow * BAG_COLS;
            int col = local % BAG_COLS;
            int row = local / BAG_COLS;
            int x = bagLeft + col * (SLOT + 4);
            int y = bagTop + row * (SLOT + 4);
            if (hit(x, y, mouseX, mouseY)) {
                return cells.get(index);
            }
        }
        return null;
    }

    public static void handleDollClick(GearSlot slot, int button) {
        if (slot == null || button != 1) {
            return;
        }
        PacketDistributor.sendToServer(new UnequipSlotPayload(slot.ordinal()));
    }

    public static void handleBagClick(BagCell cell, int button) {
        if (cell == null) {
            return;
        }
        if (button == 0 && !cell.equipped() && cell.stowedIndex() >= 0) {
            PacketDistributor.sendToServer(new EquipStowedPayload(cell.stowedIndex()));
        } else if (button == 1 && cell.equipped() && cell.gearSlot() != null) {
            PacketDistributor.sendToServer(new UnequipSlotPayload(cell.gearSlot().ordinal()));
        }
    }

    private static boolean hit(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    public record BagCell(ItemStack stack, boolean equipped, GearSlot gearSlot, int stowedIndex) {
    }
}
