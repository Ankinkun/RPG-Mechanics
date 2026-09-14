package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;
import java.util.Objects;

import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.client.ui.ClassSchoolChrome;
import com.ankin.rpgmechanics.classbuild.client.ui.ClassSchoolTheme;
import com.ankin.rpgmechanics.classbuild.client.ui.ClassSkillUi;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsClientSoft;
import com.ankin.rpgmechanics.classbuild.network.ConfirmClassBuildPayload;
import com.mojang.blaze3d.platform.Lighting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Class hub: centered spellbook hero, hover Select Spell box, tight fragments, instant apply.
 */
public class ClassEditScreen extends Screen {
    private static final int BOOK_R = 56;
    private static final int ABILITY = 40;
    private static final int ABILITY_GAP = 12;
    private static final int FRAGMENT_SLOT = 22;
    private static final int FRAGMENT_GAP = 6;
    private static final int FRAGMENT_COUNT = 6;
    private static final int FAN_HIT = 22;
    private static final int POPUP_CELL = 36;
    private static final int POPUP_PAD = 8;

    private ResourceLocation melee;
    private ResourceLocation movement;
    private ResourceLocation ranged;
    private ResourceLocation ultimate;
    private String lastSentSignature;

    private int bookCx;
    private int bookCy;
    private int abilityY;
    private int abilityStartX;
    private int playerLeft;
    private int playerTop;
    private int playerRight;
    private int playerBottom;
    private int fragLeft;
    private int fragTop;
    private int fragW;
    private int fragH;
    private int kitBottom;

    private HoverZone hover = HoverZone.NONE;
    private int hoverAbilityIndex = -1;

    private float bookFan;
    private float abilityPopup;
    private float bookPulse;

    private final ClassSchoolTheme.Fade themeFade;

    private enum HoverZone {
        NONE,
        BOOK,
        ABILITY
    }

    public ClassEditScreen() {
        super(Component.translatable("screen.rpgmechanics.edit_class"));
        ClassBuildState cached = ClassBuildClientPayloadHandlers.cached();
        this.melee = cached.melee();
        this.movement = cached.movement();
        this.ranged = cached.ranged();
        this.ultimate = cached.ultimate();
        this.themeFade = new ClassSchoolTheme.Fade(ClassSchoolTheme.forUltimate(ultimate));
        this.lastSentSignature = kitSignature();
    }

    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ClassEditScreen());
    }

    @Override
    protected void init() {
        this.clearWidgets();
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.CLASS);
        layout();
    }

    @Override
    public void tick() {
        super.tick();
        bookFan = ClassSkillUi.approach(bookFan, hover == HoverZone.BOOK ? 1f : 0f, 0.18f);
        abilityPopup = ClassSkillUi.approach(abilityPopup, hover == HoverZone.ABILITY ? 1f : 0f, 0.28f);
        bookPulse = ClassSkillUi.approach(bookPulse, hover == HoverZone.BOOK ? 1f : 0.35f, 0.12f);
        themeFade.approach(ClassSchoolTheme.forUltimate(ultimate), 0.14f);
    }

    private void layout() {
        int contentTop = RpgUiTheme.TAB_BAR_H + 8;
        int contentBottom = this.height - 16;
        int contentH = contentBottom - contentTop;

        // Kit + player as one tight group (small gap, not far-right player)
        int abilitiesW = 3 * ABILITY + 2 * ABILITY_GAP;
        int kitW = BOOK_R * 2 + 28 + abilitiesW;
        int playerW = 148;
        int kitPlayerGap = 16;
        int groupW = kitW + kitPlayerGap + playerW;
        int groupLeft = Math.max(24, (this.width - groupW) / 2);

        int kitLeft = groupLeft;
        this.bookCx = kitLeft + BOOK_R;
        int kitBlockH = BOOK_R * 2 + 40 + FRAGMENT_SLOT + 18;
        int kitTop = contentTop + Math.max(8, (contentH - kitBlockH) / 2);
        this.bookCy = kitTop + BOOK_R;

        this.abilityStartX = bookCx + BOOK_R + 28;
        this.abilityY = bookCy - ABILITY / 2;

        // Fragments: under the ability row (not under the book)
        int slotsW = FRAGMENT_COUNT * FRAGMENT_SLOT + (FRAGMENT_COUNT - 1) * FRAGMENT_GAP;
        this.fragW = Math.max(slotsW + 16, abilitiesW);
        this.fragH = FRAGMENT_SLOT + 18;
        this.fragLeft = abilityStartX + (abilitiesW - fragW) / 2;
        this.fragTop = abilityY + ABILITY + 40;
        this.kitBottom = Math.max(bookCy + BOOK_R + 8, fragTop + fragH);

        this.playerLeft = kitLeft + kitW + kitPlayerGap;
        this.playerRight = playerLeft + playerW;
        this.playerTop = kitTop;
        int desiredH = kitBottom - kitTop;
        this.playerBottom = playerTop + Math.max(desiredH, 150);
        if (playerBottom > contentBottom) {
            playerBottom = contentBottom;
        }
    }

    private String kitSignature() {
        return melee + "|" + movement + "|" + ranged + "|" + ultimate;
    }

    private void applyInstant() {
        String sig = kitSignature();
        if (Objects.equals(sig, lastSentSignature)) {
            return;
        }
        lastSentSignature = sig;
        ClassBuildState draft = new ClassBuildState(
                true,
                ClassBuildState.ClassRole.DAMAGE_DEALER,
                ClassBuildState.ElementTrack.DARKNESS,
                melee,
                movement,
                ranged,
                ultimate
        );
        int slot = ClassBuildClientPayloadHandlers.cachedRoster().activeSlot();
        if (slot < 0) {
            slot = 0;
        }
        PacketDistributor.sendToServer(new ConfirmClassBuildPayload(slot, draft));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout();
        updateHover(mouseX, mouseY);

        int accent = themeFade.accent();
        int panel = themeFade.panel();
        int panelDark = themeFade.panelDark();
        int focus = themeFade.focus();
        int glow = themeFade.glow();
        int wash = themeFade.wash();

        graphics.fill(0, 0, this.width, this.height, 0x55060A10);
        graphics.fill(0, 0, this.width, this.height, wash);
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        graphics.fill(0, RpgUiTheme.TAB_BAR_H - 1, this.width, RpgUiTheme.TAB_BAR_H, accent);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPlayerPanel(graphics, mouseX, mouseY, panel, panelDark, accent, partialTick);
        drawFragmentsStrip(graphics, panel, panelDark, accent, focus, partialTick);
        drawAbilitySlots(graphics, panel, panelDark, accent, focus, partialTick);
        drawSpellbook(graphics, panelDark, accent, glow, partialTick);

        if (bookFan > 0.01f) {
            drawBookFanOut(graphics, mouseX, mouseY, partialTick);
        }
        if (abilityPopup > 0.01f && hoverAbilityIndex >= 0) {
            drawAbilityPopup(graphics, mouseX, mouseY, panel, accent, focus);
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.darkness_dd"),
                this.width / 2,
                RpgUiTheme.TAB_BAR_H + 6,
                accent
        );
    }

    private void updateHover(int mouseX, int mouseY) {
        if (bookFan > 0.35f && hitBookFan(mouseX, mouseY) >= 0) {
            hover = HoverZone.BOOK;
            hoverAbilityIndex = -1;
            return;
        }
        if (distSq(mouseX, mouseY, bookCx, bookCy) <= (BOOK_R + 8) * (BOOK_R + 8f)) {
            hover = HoverZone.BOOK;
            hoverAbilityIndex = -1;
            return;
        }
        if (abilityPopup > 0.25f && hoverAbilityIndex >= 0 && inAbilityPopup(mouseX, mouseY)) {
            hover = HoverZone.ABILITY;
            return;
        }
        for (int i = 0; i < 3; i++) {
            int x = abilitySlotX(i);
            int y = abilityY;
            if (mouseX >= x && mouseX < x + ABILITY && mouseY >= y && mouseY < y + ABILITY) {
                hover = HoverZone.ABILITY;
                hoverAbilityIndex = i;
                return;
            }
        }
        hover = HoverZone.NONE;
        hoverAbilityIndex = -1;
    }

    private void drawPlayerPanel(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int panel,
            int panelDark,
            int accent,
            float partialTick
    ) {
        ClassSchoolChrome.catacombPlayerPanel(
                graphics,
                playerLeft,
                playerTop,
                playerRight - playerLeft,
                playerBottom - playerTop,
                panel,
                panelDark,
                accent,
                0.95f
        );
        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    playerLeft + 8,
                    playerTop + 10,
                    playerRight - 8,
                    playerBottom - 8,
                    48,
                    0.05F,
                    mouseX,
                    mouseY,
                    this.minecraft.player
            );
        }
    }

    private void drawSpellbook(GuiGraphics graphics, int panelDark, int accent, int glow, float partialTick) {
        // Hero book morphs into the fan: disc/book fade as fan opens
        float stay = 1f - ClassSkillUi.easeOutCubic(bookFan);
        float pulse = 0.65f + 0.35f * bookPulse;
        float ticks = gameTicks(partialTick);
        int glowR = (int) (BOOK_R + 12 + 8 * pulse);
        ClassSkillUi.glowDisc(
                graphics,
                bookCx,
                bookCy,
                glowR,
                ClassSkillUi.withAlpha(glow, (int) (0xFF * (0.4f + 0.6f * stay)))
        );
        if (stay > 0.05f) {
            ClassSchoolChrome.schoolRingBackdrop(
                    graphics,
                    bookCx,
                    bookCy,
                    BOOK_R,
                    ClassSchoolTheme.forUltimate(ultimate),
                    stay,
                    ticks
            );

            // ~48px tall — fills the seal without towering over it
            float bookScale = (48f + 4f * bookPulse) * stay;
            ClassSkillUi.renderSpinningItem(
                    graphics,
                    bookStack(ultimate),
                    bookCx,
                    bookCy,
                    bookScale,
                    ticks,
                    0f
            );
        }
    }

    private void drawAbilitySlots(
            GuiGraphics graphics,
            int panel,
            int panelDark,
            int accent,
            int focus,
            float partialTick
    ) {
        ResourceLocation[] selected = {melee, movement, ranged};
        String[] labels = {
                "screen.rpgmechanics.ability.melee",
                "screen.rpgmechanics.ability.movement",
                "screen.rpgmechanics.ability.ranged"
        };
        ClassSchoolTheme school = ClassSchoolTheme.forUltimate(ultimate);
        float ticks = gameTicks(partialTick);
        for (int i = 0; i < 3; i++) {
            int x = abilitySlotX(i);
            int y = abilityY;
            boolean hot = hover == HoverZone.ABILITY && hoverAbilityIndex == i;
            ClassSchoolChrome.themedSlot(
                    graphics,
                    x,
                    y,
                    ABILITY,
                    panel,
                    panelDark,
                    accent,
                    focus,
                    hot,
                    school,
                    ticks + i * 3f
            );
            drawSpellIcon(graphics, selected[i], x + 6, y + 6, 28);
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(labels[i]),
                    x + ABILITY / 2,
                    y + ABILITY + 4,
                    RpgUiTheme.TEXT_MUTED
            );
        }
    }

    private void drawFragmentsStrip(
            GuiGraphics graphics,
            int panel,
            int panelDark,
            int accent,
            int focus,
            float partialTick
    ) {
        ClassSchoolTheme school = ClassSchoolTheme.forUltimate(ultimate);
        float ticks = gameTicks(partialTick);
        ClassSchoolChrome.themedPanel(
                graphics,
                fragLeft,
                fragTop,
                fragW,
                fragH,
                panel,
                panelDark,
                accent,
                school,
                0.85f,
                ticks
        );
        graphics.drawString(
                this.font,
                Component.translatable("screen.rpgmechanics.class.fragments_soon"),
                fragLeft + 6,
                fragTop + 3,
                RpgUiTheme.TEXT_MUTED,
                false
        );
        int slotsW = FRAGMENT_COUNT * FRAGMENT_SLOT + (FRAGMENT_COUNT - 1) * FRAGMENT_GAP;
        int sx = fragLeft + Math.max(6, (fragW - slotsW) / 2);
        int sy = fragTop + 14;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            int x = sx + i * (FRAGMENT_SLOT + FRAGMENT_GAP);
            ClassSchoolChrome.themedSlot(
                    graphics,
                    x,
                    sy,
                    FRAGMENT_SLOT,
                    panel,
                    panelDark,
                    accent,
                    focus,
                    false,
                    school,
                    ticks + i * 2f
            );
        }
    }

    private void drawBookFanOut(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<ResourceLocation> ultimates = ClassBuildCatalog.ULTIMATE;
        int n = ultimates.size();
        float ticks = gameTicks(partialTick);
        float progress = bookFan;
        int selectedSlot = fanCenterSlot(n);

        // Draw non-selected first, selected last (stays visually on top / center)
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < n; slot++) {
                boolean isSelectedSlot = slot == selectedSlot;
                if (pass == 0 && isSelectedSlot) {
                    continue;
                }
                if (pass == 1 && !isSelectedSlot) {
                    continue;
                }
                int catalogIndex = fanCatalogIndex(slot, n);
                ResourceLocation ult = ultimates.get(catalogIndex);
                ClassSchoolTheme bookTheme = ClassSchoolTheme.forUltimate(ult);
                Vec2 pos = ClassSkillUi.fanArc(bookCx, bookCy, slot, n, progress, 96f);
                boolean hot = hitFanPoint(mouseX, mouseY, pos);
                float scale = (36f + (isSelectedSlot ? 4f : 0f) + (hot ? 6f : 0f))
                        * ClassSkillUi.easeOutBack(progress);

                int ix = Math.round(pos.x);
                int iy = Math.round(pos.y);
                ClassSkillUi.glowDisc(graphics, ix, iy, (int) (14 + scale * 0.15f), bookTheme.glow);
                ClassSchoolChrome.schoolRingBackdropLite(graphics, ix, iy, 16, bookTheme, progress);

                ClassSkillUi.renderSpinningItem(
                        graphics,
                        bookStack(ult),
                        pos.x,
                        pos.y,
                        scale,
                        ticks,
                        catalogIndex * 47f,
                        false
                );
            }
        }
        graphics.flush();
        Lighting.setupForFlatItems();
    }

    /** Fan slot that holds the currently selected ultimate (center of the arc). */
    private static int fanCenterSlot(int count) {
        return count / 2;
    }

    /**
     * Map a visual fan slot to a catalog index so the selected ultimate always sits in the center slot.
     */
    private int fanCatalogIndex(int slot, int count) {
        int selectedIdx = ClassBuildCatalog.ULTIMATE.indexOf(ultimate);
        if (selectedIdx < 0) {
            selectedIdx = 0;
        }
        int center = fanCenterSlot(count);
        return Math.floorMod(selectedIdx - center + slot, count);
    }

    private int hitBookFan(int mouseX, int mouseY) {
        List<ResourceLocation> ultimates = ClassBuildCatalog.ULTIMATE;
        int n = ultimates.size();
        for (int slot = 0; slot < n; slot++) {
            Vec2 pos = ClassSkillUi.fanArc(bookCx, bookCy, slot, n, Math.max(bookFan, 0.5f), 96f);
            if (hitFanPoint(mouseX, mouseY, pos)) {
                return fanCatalogIndex(slot, n);
            }
        }
        return -1;
    }

    private void drawAbilityPopup(GuiGraphics graphics, int mouseX, int mouseY, int panel, int accent, int focus) {
        float t = ClassSkillUi.easeOutCubic(abilityPopup);
        if (t < 0.05f) {
            return;
        }
        List<ResourceLocation> options = abilityOptions(hoverAbilityIndex);
        int cols = Math.min(3, Math.max(1, options.size()));
        int rows = (options.size() + cols - 1) / cols;
        int pw = cols * (POPUP_CELL + 4) + POPUP_PAD * 2;
        int ph = rows * (POPUP_CELL + 4) + POPUP_PAD * 2 + 14;
        int popupX = abilitySlotX(hoverAbilityIndex);
        int popupY = abilityY + ABILITY + 16;

        ClassSchoolChrome.themedPanel(
                graphics,
                popupX,
                popupY,
                pw,
                ph,
                ClassSkillUi.withAlpha(panel, (int) (0xFF * t)),
                ClassSkillUi.withAlpha(0x101018, (int) (0xF2 * t)),
                ClassSkillUi.withAlpha(accent, (int) (0xFF * t)),
                ClassSchoolTheme.forUltimate(ultimate),
                t,
                gameTicks(0f)
        );
        graphics.drawString(
                this.font,
                Component.translatable("screen.rpgmechanics.class.select_spell"),
                popupX + POPUP_PAD,
                popupY + 4,
                ClassSkillUi.withAlpha(RpgUiTheme.TEXT_MUTED, (int) (0xFF * t)),
                false
        );

        for (int i = 0; i < options.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = popupX + POPUP_PAD + col * (POPUP_CELL + 4);
            int y = popupY + POPUP_PAD + 12 + row * (POPUP_CELL + 4);
            ResourceLocation id = options.get(i);
            boolean selected = id.equals(selectedAbility(hoverAbilityIndex));
            boolean hot = mouseX >= x && mouseX < x + POPUP_CELL && mouseY >= y && mouseY < y + POPUP_CELL;
            ClassSkillUi.fillRounded(graphics, x, y, POPUP_CELL, POPUP_CELL, 3, ClassSkillUi.withAlpha(panel, 0xEE));
            ClassSkillUi.outlineRounded(graphics, x, y, POPUP_CELL, POPUP_CELL, 3, selected || hot ? focus : accent);
            drawSpellIcon(graphics, id, x + 2, y + 2, 32);
        }
    }

    private boolean inAbilityPopup(int mouseX, int mouseY) {
        if (hoverAbilityIndex < 0) {
            return false;
        }
        List<ResourceLocation> options = abilityOptions(hoverAbilityIndex);
        int cols = Math.min(3, Math.max(1, options.size()));
        int rows = (options.size() + cols - 1) / cols;
        int pw = cols * (POPUP_CELL + 4) + POPUP_PAD * 2;
        int ph = rows * (POPUP_CELL + 4) + POPUP_PAD * 2 + 14;
        int popupX = abilitySlotX(hoverAbilityIndex);
        int popupY = abilityY + ABILITY + 16;
        return mouseX >= popupX && mouseX < popupX + pw && mouseY >= popupY && mouseY < popupY + ph;
    }

    private static boolean hitFanPoint(int mouseX, int mouseY, Vec2 pos) {
        return Math.abs(mouseX - pos.x) <= FAN_HIT && Math.abs(mouseY - pos.y) <= FAN_HIT;
    }

    private int abilitySlotX(int index) {
        return abilityStartX + index * (ABILITY + ABILITY_GAP);
    }

    private List<ResourceLocation> abilityOptions(int index) {
        return switch (index) {
            case 0 -> ClassBuildCatalog.MELEE;
            case 1 -> ClassBuildCatalog.MOVEMENT;
            case 2 -> ClassBuildCatalog.RANGED;
            default -> List.of();
        };
    }

    private ResourceLocation selectedAbility(int index) {
        return switch (index) {
            case 0 -> melee;
            case 1 -> movement;
            case 2 -> ranged;
            default -> melee;
        };
    }

    private void setSelectedAbility(int index, ResourceLocation id) {
        switch (index) {
            case 0 -> melee = id;
            case 1 -> movement = id;
            case 2 -> ranged = id;
            default -> {
            }
        }
    }

    private static ItemStack bookStack(ResourceLocation ultimate) {
        ResourceLocation bookId = ClassBuildCatalog.bookForUltimate(ultimate);
        Item item = BuiltInRegistries.ITEM.get(bookId);
        return item == Items.AIR ? new ItemStack(Items.BOOK) : new ItemStack(item);
    }

    private void drawSpellIcon(GuiGraphics graphics, ResourceLocation spellId, int x, int y, int size) {
        ResourceLocation icon = IronSpellsClientSoft.getSpellIcon(spellId);
        if (icon != null) {
            graphics.blit(icon, x, y, 0, 0, size, size, size, size);
        } else {
            graphics.fill(x, y, x + size, y + size, 0xFF222230);
            String letter = ClassBuildCatalog.displayName(spellId);
            if (!letter.isEmpty()) {
                graphics.drawCenteredString(
                        this.font,
                        letter.substring(0, 1),
                        x + size / 2,
                        y + size / 2 - 4,
                        RpgUiTheme.TEXT_MUTED
                );
            }
        }
    }

    private float gameTicks(float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            return this.minecraft.level.getGameTime() + partialTick;
        }
        return partialTick;
    }

    private static double distSq(int x, int y, int cx, int cy) {
        long dx = x - cx;
        long dy = y - cy;
        return dx * dx + dy * dy;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        layout();
        updateHover((int) mouseX, (int) mouseY);

        if (bookFan > 0.3f) {
            int hit = hitBookFan((int) mouseX, (int) mouseY);
            if (hit >= 0) {
                ultimate = ClassBuildCatalog.ULTIMATE.get(hit);
                applyInstant();
                return true;
            }
        }

        if (abilityPopup > 0.25f && hoverAbilityIndex >= 0 && inAbilityPopup((int) mouseX, (int) mouseY)) {
            List<ResourceLocation> options = abilityOptions(hoverAbilityIndex);
            int cols = Math.min(3, Math.max(1, options.size()));
            int popupX = abilitySlotX(hoverAbilityIndex);
            int popupY = abilityY + ABILITY + 16;
            for (int i = 0; i < options.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int x = popupX + POPUP_PAD + col * (POPUP_CELL + 4);
                int y = popupY + POPUP_PAD + 12 + row * (POPUP_CELL + 4);
                if (mouseX >= x && mouseX < x + POPUP_CELL && mouseY >= y && mouseY < y + POPUP_CELL) {
                    setSelectedAbility(hoverAbilityIndex, options.get(i));
                    applyInstant();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
