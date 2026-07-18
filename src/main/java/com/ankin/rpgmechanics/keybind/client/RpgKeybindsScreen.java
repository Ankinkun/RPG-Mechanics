package com.ankin.rpgmechanics.keybind.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.keybind.BindingOverride;
import com.ankin.rpgmechanics.keybind.CategoryDef;
import com.ankin.rpgmechanics.keybind.KeyChord;
import com.ankin.rpgmechanics.keybind.KeyTriggerMode;
import com.ankin.rpgmechanics.keybind.KeybindCatalog;
import com.ankin.rpgmechanics.keybind.KeybindManager;
import com.ankin.rpgmechanics.keybind.KeybindPermissions;
import com.ankin.rpgmechanics.keybind.KeybindProfile;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Vanilla-style keybinds list: ~50% name, ~50% Primary | Secondary | Trigger | Reset.
 */
public class RpgKeybindsScreen extends Screen {
    private final Screen lastScreen;
    private final Options options;

    private BindingList bindingList;
    private EditBox searchBox;

    @Nullable
    private String listeningBinding;
    private int listeningSlot = -1; // 0 primary, 1 secondary
    private KeyModifier captureModifier = KeyModifier.NONE;

    public RpgKeybindsScreen(Screen lastScreen, Options options) {
        super(Component.translatable("screen.rpgmechanics.keybinds"));
        this.lastScreen = lastScreen;
        this.options = options;
    }

    @Override
    protected void init() {
        KeybindCatalog.refresh();
        int top = 32;
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 8, 200, 16, Component.translatable("screen.rpgmechanics.keybinds_search"));
        this.searchBox.setResponder(text -> rebuildList());
        this.addRenderableWidget(this.searchBox);

        int listTop = top;
        if (KeybindPermissions.isAuthoringEnabled()) {
            int y = top;
            this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.keybinds_new_category"), b -> createCategory())
                    .bounds(this.width / 2 - 154, y, 100, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.keybinds_reload"), b -> {
                KeybindManager.reloadFromDisk();
                rebuildList();
            }).bounds(this.width / 2 - 50, y, 100, 20).build());
            listTop = top + 24;
        }

        this.bindingList = new BindingList(this.width, this.height - listTop - 32, listTop);
        this.addRenderableWidget(this.bindingList);

        this.addRenderableWidget(Button.builder(Component.translatable("controls.resetAll"), b -> {
            KeybindManager.resetAllToDefaults();
            rebuildList();
        }).bounds(this.width / 2 - 155, this.height - 26, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 + 5, this.height - 26, 150, 20).build());

        rebuildList();
    }

    private void rebuildList() {
        String filter = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        if (this.bindingList != null) {
            this.bindingList.reload(filter);
        }
    }

    public void refreshButtons() {
        if (this.bindingList != null) {
            this.bindingList.children().forEach(entry -> {
                if (entry instanceof BindingEntry bindingEntry) {
                    bindingEntry.refreshButtons();
                }
            });
        }
    }

    private void beginListen(String bindingName, int slot) {
        this.listeningBinding = bindingName;
        this.listeningSlot = slot;
        this.captureModifier = KeyModifier.NONE;
        refreshButtons();
    }

    private void applyCapturedKey(InputConstants.Key key) {
        if (this.listeningBinding == null || this.listeningSlot < 0) {
            return;
        }
        KeyMapping mapping = KeybindCatalog.get(this.listeningBinding);
        if (mapping != null) {
            KeybindManager.setChordSlot(mapping, this.listeningSlot, key, this.captureModifier);
        }
        this.listeningBinding = null;
        this.listeningSlot = -1;
        this.captureModifier = KeyModifier.NONE;
        rebuildList();
    }

    private void createCategory() {
        String id = "custom_" + (KeybindManager.profile().categories().size() + 1);
        CategoryDef category = new CategoryDef(id, "Custom " + (KeybindManager.profile().categories().size() + 1), List.of());
        KeybindManager.setProfile(KeybindManager.profile().upsertCategory(category));
        rebuildList();
    }

    private void cycleCategoryFor(KeyMapping mapping) {
        if (!KeybindPermissions.isAuthoringEnabled()) {
            return;
        }
        KeybindProfile profile = KeybindManager.profile();
        List<CategoryDef> categories = profile.categories();
        if (categories.isEmpty()) {
            return;
        }
        KeybindManager.ensureOverride(mapping);
        BindingOverride current = KeybindManager.effectiveOverride(mapping);
        Optional<String> currentCat = current.customCategory();
        int index = -1;
        if (currentCat.isPresent()) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id().equals(currentCat.get())) {
                    index = i;
                    break;
                }
            }
        }
        int nextIndex = index + 1;
        BindingOverride next;
        KeybindProfile nextProfile;
        if (nextIndex >= categories.size()) {
            next = current.withCustomCategory(Optional.empty());
            List<CategoryDef> cleaned = new ArrayList<>();
            for (CategoryDef category : categories) {
                cleaned.add(category.removeEntry(mapping.getName()));
            }
            nextProfile = profile.withCategories(cleaned).withBinding(mapping.getName(), next);
        } else {
            CategoryDef target = categories.get(nextIndex);
            next = current.withCustomCategory(Optional.of(target.id()));
            List<CategoryDef> updated = new ArrayList<>();
            for (CategoryDef category : categories) {
                CategoryDef edited = category.removeEntry(mapping.getName());
                if (category.id().equals(target.id())) {
                    edited = edited.addEntry(mapping.getName());
                }
                updated.add(edited);
            }
            nextProfile = profile.withCategories(updated).withBinding(mapping.getName(), next);
        }
        KeybindManager.setProfile(nextProfile);
        rebuildList();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.listeningBinding != null) {
            if (keyCode == 256) {
                applyCapturedKey(InputConstants.UNKNOWN);
                return true;
            }
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            if (key.getValue() == 340 || key.getValue() == 344) {
                this.captureModifier = KeyModifier.SHIFT;
                return true;
            }
            if (key.getValue() == 341 || key.getValue() == 345) {
                this.captureModifier = KeyModifier.CONTROL;
                return true;
            }
            if (key.getValue() == 342 || key.getValue() == 346) {
                this.captureModifier = KeyModifier.ALT;
                return true;
            }
            applyCapturedKey(key);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.listeningBinding != null) {
            applyCapturedKey(InputConstants.Type.MOUSE.getOrCreate(button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 2, 0xFFFFFF);
    }

    private static Component triggerLabel(KeyTriggerMode mode) {
        return Component.translatable("screen.rpgmechanics.keybinds_trigger_" + mode.wireName());
    }

    private static Component chordButtonLabel(Optional<KeyChord> chord) {
        if (chord.isEmpty() || chord.get().key().equals("key.keyboard.unknown")) {
            return Component.translatable("screen.rpgmechanics.keybinds_unbound");
        }
        KeyChord value = chord.get();
        InputConstants.Key key = value.resolveKey().orElse(InputConstants.UNKNOWN);
        if (value.modifier() != KeyModifier.NONE) {
            return Component.literal(value.modifier().name().charAt(0) + "+" + key.getDisplayName().getString());
        }
        return key.getDisplayName();
    }

    private class BindingList extends ContainerObjectSelectionList<Entry> {
        private BindingList(int width, int height, int y) {
            super(Minecraft.getInstance(), width, height, y, 20);
        }

        private void reload(String filter) {
            this.clearEntries();
            List<KeyMapping> mappings = new ArrayList<>(KeybindCatalog.all());
            // Category order follows profile JSON list order; bindings follow that category's entries list.
            mappings.sort(Comparator
                    .comparingInt(KeybindManager::categoryOrderIndex)
                    .thenComparingInt(KeybindManager::bindingOrderIndex)
                    .thenComparing(KeybindCatalog::displayNameString, String.CASE_INSENSITIVE_ORDER));

            String lastHeader = null;
            for (KeyMapping mapping : mappings) {
                boolean authoring = KeybindPermissions.isAuthoringEnabled();
                if (!authoring && !KeybindManager.isVisible(mapping)) {
                    continue;
                }
                String label = KeybindCatalog.displayNameString(mapping);
                String category = KeybindManager.resolveDisplayCategory(mapping)
                        .orElse(KeybindCatalog.displayCategoryString(mapping.getCategory(), null));
                if (filter != null && !filter.isEmpty()) {
                    String hay = (label + " " + category + " " + mapping.getName()).toLowerCase(Locale.ROOT);
                    if (!hay.contains(filter)) {
                        continue;
                    }
                }
                if (!category.equals(lastHeader)) {
                    this.addEntry(new CategoryEntry(category));
                    lastHeader = category;
                }
                this.addEntry(new BindingEntry(mapping));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(this.width - 20, 480);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 6;
        }
    }

    private abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    private class CategoryEntry extends Entry {
        private final String title;

        private CategoryEntry(String title) {
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            graphics.drawCenteredString(RpgKeybindsScreen.this.font, this.title, left + width / 2, top + 5, 0xFFFF55);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }
    }

    private class BindingEntry extends Entry {
        private final KeyMapping mapping;
        private final Button primaryButton;
        private final Button secondaryButton;
        private final Button triggerButton;
        private final Button resetButton;
        @Nullable
        private final Button visibilityButton;
        @Nullable
        private final Button categoryButton;

        private BindingEntry(KeyMapping mapping) {
            this.mapping = mapping;
            this.primaryButton = Button.builder(Component.empty(), b -> RpgKeybindsScreen.this.beginListen(mapping.getName(), 0)).bounds(0, 0, 50, 20).build();
            this.secondaryButton = Button.builder(Component.empty(), b -> RpgKeybindsScreen.this.beginListen(mapping.getName(), 1)).bounds(0, 0, 50, 20).build();
            this.triggerButton = Button.builder(Component.empty(), b -> {
                KeybindManager.cycleTrigger(mapping);
                RpgKeybindsScreen.this.rebuildList();
            }).bounds(0, 0, 50, 20).build();
            this.resetButton = Button.builder(Component.translatable("controls.reset"), b -> {
                KeybindManager.resetBinding(mapping);
                RpgKeybindsScreen.this.rebuildList();
            }).bounds(0, 0, 50, 20).build();
            if (KeybindPermissions.isAuthoringEnabled()) {
                this.visibilityButton = Button.builder(Component.empty(), b -> {
                    KeybindManager.toggleVisible(mapping);
                    RpgKeybindsScreen.this.rebuildList();
                }).bounds(0, 0, 36, 20).build();
                this.categoryButton = Button.builder(Component.literal("…"), b -> RpgKeybindsScreen.this.cycleCategoryFor(mapping))
                        .bounds(0, 0, 16, 20).build();
            } else {
                this.visibilityButton = null;
                this.categoryButton = null;
            }
            refreshButtons();
        }

        private void refreshButtons() {
            boolean listeningPrimary = mapping.getName().equals(RpgKeybindsScreen.this.listeningBinding)
                    && RpgKeybindsScreen.this.listeningSlot == 0;
            boolean listeningSecondary = mapping.getName().equals(RpgKeybindsScreen.this.listeningBinding)
                    && RpgKeybindsScreen.this.listeningSlot == 1;
            this.primaryButton.setMessage(listeningPrimary
                    ? Component.translatable("screen.rpgmechanics.keybinds_listening").withStyle(ChatFormatting.YELLOW)
                    : chordButtonLabel(KeybindManager.primaryChord(mapping)));
            this.secondaryButton.setMessage(listeningSecondary
                    ? Component.translatable("screen.rpgmechanics.keybinds_listening").withStyle(ChatFormatting.YELLOW)
                    : chordButtonLabel(KeybindManager.secondaryChord(mapping)));
            this.triggerButton.setMessage(triggerLabel(KeybindManager.effectiveTrigger(mapping)));
            if (this.visibilityButton != null) {
                this.visibilityButton.setMessage(KeybindManager.isVisible(this.mapping)
                        ? Component.translatable("screen.rpgmechanics.keybinds_hide")
                        : Component.translatable("screen.rpgmechanics.keybinds_show"));
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int half = width / 2;
            int buttonArea = half;
            int buttonWidth = Math.max(40, (buttonArea - 6) / 4);
            int gap = 2;
            int bx = left + half;
            int nameWidth = half - 8;
            if (this.visibilityButton != null && this.categoryButton != null) {
                nameWidth = half - 58;
            }

            String name = KeybindCatalog.displayNameString(this.mapping);
            if (!KeybindManager.isEnabled(this.mapping)) {
                name = name + " [off]";
            }
            if (KeybindPermissions.isAuthoringEnabled() && !KeybindManager.isVisible(this.mapping)) {
                name = name + " [hidden]";
            }
            int nameColor = hovering ? 0xFFFFA0 : 0xFFFFFF;
            if (KeybindPermissions.isAuthoringEnabled() && !KeybindManager.isVisible(this.mapping)) {
                nameColor = 0xFF888888;
            }
            graphics.drawString(
                    RpgKeybindsScreen.this.font,
                    RpgKeybindsScreen.this.font.plainSubstrByWidth(name, nameWidth),
                    left + 2,
                    top + 6,
                    nameColor,
                    false
            );

            this.primaryButton.setX(bx);
            this.primaryButton.setY(top);
            this.primaryButton.setWidth(buttonWidth);
            this.secondaryButton.setX(bx + buttonWidth + gap);
            this.secondaryButton.setY(top);
            this.secondaryButton.setWidth(buttonWidth);
            this.triggerButton.setX(bx + 2 * (buttonWidth + gap));
            this.triggerButton.setY(top);
            this.triggerButton.setWidth(buttonWidth);
            this.resetButton.setX(bx + 3 * (buttonWidth + gap));
            this.resetButton.setY(top);
            this.resetButton.setWidth(buttonWidth);

            this.primaryButton.render(graphics, mouseX, mouseY, partialTick);
            this.secondaryButton.render(graphics, mouseX, mouseY, partialTick);
            this.triggerButton.render(graphics, mouseX, mouseY, partialTick);
            this.resetButton.render(graphics, mouseX, mouseY, partialTick);
            if (this.visibilityButton != null && this.categoryButton != null) {
                this.visibilityButton.setX(left + half - 56);
                this.visibilityButton.setY(top);
                this.categoryButton.setX(left + half - 18);
                this.categoryButton.setY(top);
                this.visibilityButton.render(graphics, mouseX, mouseY, partialTick);
                this.categoryButton.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            if (this.visibilityButton != null && this.categoryButton != null) {
                return List.of(
                        this.primaryButton,
                        this.secondaryButton,
                        this.triggerButton,
                        this.resetButton,
                        this.visibilityButton,
                        this.categoryButton
                );
            }
            return List.of(this.primaryButton, this.secondaryButton, this.triggerButton, this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            List<NarratableEntry> list = new ArrayList<>();
            list.add(this.primaryButton);
            list.add(this.secondaryButton);
            list.add(this.triggerButton);
            list.add(this.resetButton);
            if (this.visibilityButton != null) {
                list.add(this.visibilityButton);
            }
            if (this.categoryButton != null) {
                list.add(this.categoryButton);
            }
            return list;
        }
    }
}
