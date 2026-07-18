package com.ankin.rpgmechanics.quest.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.ankin.rpgmechanics.quest.QuestDefinition;
import com.ankin.rpgmechanics.quest.QuestStep;
import com.ankin.rpgmechanics.quest.network.EditorDeleteQuestPayload;
import com.ankin.rpgmechanics.quest.network.EditorSaveQuestPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.Criterion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Two-mode authoring UI: browse/create quests, then edit one at a time.
 * Icon picking uses a dedicated opaque modal with an icon grid.
 */
public class QuestEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 330;
    private static final Gson COMPACT_JSON = new GsonBuilder().disableHtmlEscaping().create();

    private enum Mode {
        LIST,
        EDIT
    }

    private Mode mode = Mode.LIST;
    private int panelLeft;
    private int panelTop;

    private QuestBrowserList browserList;
    private ResourceLocation listSelection;

    private EditBox idBox;
    private EditBox titleBox;
    private EditBox descriptionBox;
    private EditBox iconBox;
    private EditBox stepIdBox;
    private EditBox stepTitleBox;
    private EditBox stepDescriptionBox;
    private EditBox conditionsBox;
    private EditBox iconSearchBox;
    private ScrollableDropdown<ResourceLocation> triggerDropdown;
    private StepList stepList;
    private IconGridBrowser iconGrid;
    private boolean iconBrowserOpen;

    private final List<QuestStep> editingSteps = new ArrayList<>();
    private final List<String> editingStepConditions = new ArrayList<>();
    private final List<ResourceLocation> triggerIds = new ArrayList<>();
    private String editingIconValue = "minecraft:writable_book";
    private String editingTitleValue = "Quest Title";
    private String editingDescriptionValue = "Description";
    private String editingIdValue = "rpgmechanics:my_quest";
    private boolean editingRepeatable;
    private ResourceLocation editingId;
    private boolean creatingNew;
    private int selectedStepIndex = -1;
    private boolean draftingNewStep;
    private boolean suppressTriggerTemplate;
    private boolean dirty;
    private boolean suppressDirty;
    private Component statusMessage = Component.empty();
    private int statusColor = 0x55FF55;

    public QuestEditorScreen() {
        super(Component.translatable("screen.rpgmechanics.quest_editor"));
    }

    public void refreshFromCache() {
        if (this.mode != Mode.LIST) {
            return;
        }
        ResourceLocation keepList = this.listSelection;
        this.rebuildWidgets();
        this.listSelection = keepList;
        if (this.browserList != null && keepList != null) {
            this.browserList.selectId(keepList);
        }
    }

    @Override
    protected void init() {
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - PANEL_HEIGHT) / 2;

        this.triggerIds.clear();
        this.triggerIds.addAll(DetectionMethods.triggerIds());

        if (this.mode == Mode.LIST) {
            initListMode();
        } else if (this.iconBrowserOpen) {
            initIconModal();
        } else {
            initEditForm();
        }
    }

    private void initListMode() {
        this.browserList = new QuestBrowserList(150, PANEL_HEIGHT - 56, this.panelTop + 28);
        this.browserList.setX(this.panelLeft + 8);
        this.addRenderableWidget(this.browserList);
        if (this.listSelection != null) {
            this.browserList.selectId(this.listSelection);
        }

        int bottom = this.panelTop + PANEL_HEIGHT - 26;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.create_quest"), b -> beginCreate())
                .bounds(this.panelLeft + 8, bottom, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.edit_quest"), b -> beginEditSelected())
                .bounds(this.panelLeft + 102, bottom, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.delete"), b -> deleteFromList())
                .bounds(this.panelLeft + 176, bottom, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.panelLeft + PANEL_WIDTH - 78, bottom, 70, 20).build());
    }

    /** Opaque icon picker only — no form widgets underneath. */
    private void initIconModal() {
        QuestIcons.ensureCustomIconsLoaded();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> closeIconBrowser())
                .bounds(this.panelLeft + PANEL_WIDTH - 78, this.panelTop + 6, 70, 16).build());

        this.iconSearchBox = new EditBox(this.font, this.panelLeft + 12, this.panelTop + 28, PANEL_WIDTH - 24, 16, Component.empty());
        this.iconSearchBox.setMaxLength(128);
        this.iconSearchBox.setHint(Component.translatable("screen.rpgmechanics.icon_search"));
        this.iconSearchBox.setResponder(text -> {
            if (this.iconGrid != null) {
                this.iconGrid.reload(text);
            }
        });
        this.addRenderableWidget(this.iconSearchBox);

        this.iconGrid = new IconGridBrowser(
                this.panelLeft + 12,
                this.panelTop + 50,
                PANEL_WIDTH - 24,
                PANEL_HEIGHT - 70,
                this::selectIcon
        );
        this.addRenderableWidget(this.iconGrid);
        this.iconGrid.reload("");
    }

    private void initEditForm() {
        QuestIcons.ensureCustomIconsLoaded();

        int left = this.panelLeft + 8;
        int right = this.panelLeft + 170;
        int fieldW = 294;
        int headingBottom = this.panelTop + 20;
        int questTop = headingBottom + 12;
        int stepTop = this.panelTop + 158;
        int detectTop = this.panelTop + 246;

        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.back_to_list"), b -> requestBackToList())
                .bounds(this.panelLeft + PANEL_WIDTH - 78, this.panelTop + 4, 70, 16).build());

        this.stepList = new StepList(150, 138, questTop);
        this.stepList.setX(left);
        this.addRenderableWidget(this.stepList);

        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> beginNewStep())
                .bounds(left, questTop + 142, 24, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> removeSelectedStep())
                .bounds(left + 28, questTop + 142, 24, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> moveSelectedStep(-1))
                .bounds(left + 56, questTop + 142, 24, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> moveSelectedStep(1))
                .bounds(left + 84, questTop + 142, 24, 16).build());

        this.idBox = fieldEdit(right, questTop, fieldW, this.editingIdValue);
        this.titleBox = fieldEdit(right, questTop + 30, fieldW, this.editingTitleValue);
        this.descriptionBox = fieldEdit(right, questTop + 60, fieldW - 40, this.editingDescriptionValue);
        this.iconBox = fieldEdit(right, questTop + 90, fieldW - 56, this.editingIconValue);
        this.idBox.setEditable(this.creatingNew);
        this.idBox.setResponder(v -> {
            this.editingIdValue = v;
            markDirty();
        });
        this.titleBox.setResponder(v -> {
            this.editingTitleValue = v;
            markDirty();
        });
        this.descriptionBox.setResponder(v -> {
            this.editingDescriptionValue = v;
            markDirty();
        });
        this.iconBox.setResponder(v -> {
            this.editingIconValue = v;
            markDirty();
        });

        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.pick_icon"), b -> openIconBrowser())
                .bounds(right + fieldW - 52, questTop + 100, 52, 16).build());

        this.stepIdBox = fieldEdit(right, stepTop, 90, "step_1");
        this.stepTitleBox = fieldEdit(right + 96, stepTop, fieldW - 96, "Step title");
        this.stepDescriptionBox = fieldEdit(right, stepTop + 30, fieldW, "Step description");

        ResourceLocation initialTrigger = this.triggerIds.contains(ResourceLocation.withDefaultNamespace("inventory_changed"))
                ? ResourceLocation.withDefaultNamespace("inventory_changed")
                : this.triggerIds.getFirst();
        this.triggerDropdown = new ScrollableDropdown<>(
                right,
                detectTop,
                fieldW,
                16,
                Component.translatable("screen.rpgmechanics.trigger"),
                this.triggerIds,
                initialTrigger,
                DetectionMethods::labelFor
        );
        this.triggerDropdown.setOnChanged(() -> {
            if (!this.suppressTriggerTemplate) {
                applyTriggerExample(this.triggerDropdown.getValue());
                markDirty();
            }
        });
        this.addRenderableWidget(this.triggerDropdown);

        this.conditionsBox = fieldEdit(right, detectTop + 28, fieldW, "{}");
        this.conditionsBox.setMaxLength(2000);
        this.stepIdBox.setResponder(v -> markDirty());
        this.stepTitleBox.setResponder(v -> markDirty());
        this.stepDescriptionBox.setResponder(v -> markDirty());
        this.conditionsBox.setResponder(v -> markDirty());

        int bottom = this.panelTop + PANEL_HEIGHT - 22;
        this.addRenderableWidget(Button.builder(repeatableButtonLabel(), b -> {
            this.editingRepeatable = !this.editingRepeatable;
            b.setMessage(repeatableButtonLabel());
            markDirty();
        }).bounds(this.panelLeft + 8, bottom, 140, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.save"), b -> save())
                .bounds(this.panelLeft + PANEL_WIDTH - 154, bottom, 70, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.delete"), b -> deleteCurrent())
                .bounds(this.panelLeft + PANEL_WIDTH - 78, bottom, 70, 16).build());

        if (this.selectedStepIndex >= 0 && this.selectedStepIndex < this.editingSteps.size()) {
            loadStepIntoForm(this.selectedStepIndex);
            this.draftingNewStep = false;
        } else {
            beginNewStep();
        }
        this.stepList.reload();
        if (this.selectedStepIndex >= 0) {
            this.stepList.selectIndex(this.selectedStepIndex);
        }
    }

    private EditBox fieldEdit(int x, int labelY, int width, String value) {
        EditBox box = new EditBox(this.font, x, labelY + 10, width, 14, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        return this.addRenderableWidget(box);
    }

    private void openIconBrowser() {
        if (this.iconBox != null) {
            this.editingIconValue = this.iconBox.getValue();
        }
        if (this.idBox != null) {
            this.editingIdValue = this.idBox.getValue();
            this.editingTitleValue = this.titleBox.getValue();
            this.editingDescriptionValue = this.descriptionBox.getValue();
        }
        this.iconBrowserOpen = true;
        this.rebuildWidgets();
    }

    private void closeIconBrowser() {
        this.iconBrowserOpen = false;
        this.rebuildWidgets();
    }

    private Component repeatableButtonLabel() {
        return Component.translatable(this.editingRepeatable
                ? "screen.rpgmechanics.repeatable_yes"
                : "screen.rpgmechanics.repeatable_no");
    }

    private void markDirty() {
        if (!this.suppressDirty) {
            this.dirty = true;
        }
    }

    private void selectIcon(ResourceLocation icon) {
        this.editingIconValue = icon.toString();
        this.iconBrowserOpen = false;
        markDirty();
        setStatus(Component.translatable("screen.rpgmechanics.icon_set"), 0x55FF55);
        this.rebuildWidgets();
    }

    private void setStatus(Component message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    private void beginCreate() {
        this.mode = Mode.EDIT;
        this.creatingNew = true;
        this.editingId = null;
        this.editingSteps.clear();
        this.editingStepConditions.clear();
        this.selectedStepIndex = -1;
        this.draftingNewStep = true;
        this.iconBrowserOpen = false;
        this.editingIdValue = "rpgmechanics:my_quest";
        this.editingTitleValue = "Quest Title";
        this.editingDescriptionValue = "Description";
        this.editingIconValue = "minecraft:writable_book";
        this.editingRepeatable = false;
        this.statusMessage = Component.empty();
        this.statusColor = 0x55FF55;
        this.dirty = false;
        this.rebuildWidgets();
    }

    private void beginEditSelected() {
        if (this.listSelection == null) {
            setStatus(Component.translatable("screen.rpgmechanics.select_quest_first"), 0xFF5555);
            return;
        }
        QuestDefinition quest = ClientQuestCache.get(this.listSelection).orElse(null);
        if (quest == null) {
            setStatus(Component.translatable("screen.rpgmechanics.select_quest_first"), 0xFF5555);
            return;
        }
        openEdit(quest);
    }

    private void openEdit(QuestDefinition quest) {
        this.mode = Mode.EDIT;
        this.creatingNew = false;
        this.editingId = quest.id();
        this.editingIdValue = quest.id().toString();
        this.editingTitleValue = quest.title();
        this.editingDescriptionValue = quest.description();
        this.editingIconValue = quest.icon().map(ResourceLocation::toString).orElse("minecraft:writable_book");
        this.editingRepeatable = quest.repeatable();
        this.editingSteps.clear();
        this.editingStepConditions.clear();
        for (QuestStep step : quest.steps()) {
            this.editingSteps.add(step);
            this.editingStepConditions.add(extractConditionsJson(step));
        }
        this.selectedStepIndex = this.editingSteps.isEmpty() ? -1 : 0;
        this.draftingNewStep = false;
        this.iconBrowserOpen = false;
        this.statusMessage = Component.translatable("screen.rpgmechanics.editing_quest", quest.title());
        this.statusColor = 0x55FF55;
        this.dirty = false;
        this.rebuildWidgets();
    }

    private void requestBackToList() {
        if (!this.dirty) {
            backToList();
            return;
        }
        this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        this.dirty = false;
                        backToList();
                    } else {
                        this.minecraft.setScreen(this);
                    }
                },
                Component.translatable("screen.rpgmechanics.unsaved_title"),
                Component.translatable("screen.rpgmechanics.unsaved_message")
        ));
    }

    @Override
    public void onClose() {
        if (this.mode == Mode.EDIT && this.dirty) {
            this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            this.dirty = false;
                            super.onClose();
                        } else {
                            this.minecraft.setScreen(this);
                        }
                    },
                    Component.translatable("screen.rpgmechanics.unsaved_title"),
                    Component.translatable("screen.rpgmechanics.unsaved_message")
            ));
            return;
        }
        super.onClose();
    }

    private void backToList() {
        this.mode = Mode.LIST;
        this.creatingNew = false;
        this.editingId = null;
        this.editingSteps.clear();
        this.editingStepConditions.clear();
        this.selectedStepIndex = -1;
        this.iconBrowserOpen = false;
        this.dirty = false;
        this.statusMessage = Component.empty();
        this.rebuildWidgets();
    }

    private String nextUniqueStepId() {
        int n = 1;
        while (true) {
            String candidate = "step_" + n;
            boolean taken = false;
            for (QuestStep step : this.editingSteps) {
                if (step.id().equals(candidate)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return candidate;
            }
            n++;
        }
    }

    private boolean isStepIdTaken(String stepId, int ignoreIndex) {
        for (int i = 0; i < this.editingSteps.size(); i++) {
            if (i == ignoreIndex) {
                continue;
            }
            if (this.editingSteps.get(i).id().equals(stepId)) {
                return true;
            }
        }
        return false;
    }

    private void beginNewStep() {
        this.draftingNewStep = true;
        this.selectedStepIndex = -1;
        markDirty();
        if (this.stepList != null) {
            this.stepList.setSelected(null);
        }
        if (this.stepIdBox != null) {
            this.stepIdBox.setValue(nextUniqueStepId());
            this.stepTitleBox.setValue("Step title");
            this.stepDescriptionBox.setValue("Step description");
            ResourceLocation inventory = ResourceLocation.withDefaultNamespace("inventory_changed");
            this.suppressTriggerTemplate = true;
            if (this.triggerIds.contains(inventory)) {
                this.triggerDropdown.setValue(inventory);
            }
            this.suppressTriggerTemplate = false;
            applyTriggerExample(this.triggerDropdown.getValue());
        }
        setStatus(Component.translatable("screen.rpgmechanics.drafting_step"), 0xA0A0A0);
    }

    private void loadStepIntoForm(int index) {
        if (index < 0 || index >= this.editingSteps.size() || this.stepIdBox == null) {
            return;
        }
        QuestStep step = this.editingSteps.get(index);
        this.suppressDirty = true;
        this.stepIdBox.setValue(step.id());
        this.stepTitleBox.setValue(step.title());
        this.stepDescriptionBox.setValue(step.description() == null ? "" : step.description());

        ResourceLocation trigger = BuiltInRegistries.TRIGGER_TYPES.getKey(step.criterion().trigger());
        this.suppressTriggerTemplate = true;
        if (trigger != null && this.triggerIds.contains(trigger)) {
            this.triggerDropdown.setValue(trigger);
        } else if (!this.triggerIds.isEmpty()) {
            this.triggerDropdown.setValue(this.triggerIds.getFirst());
        }
        this.suppressTriggerTemplate = false;

        String conditions = index < this.editingStepConditions.size()
                ? this.editingStepConditions.get(index)
                : extractConditionsJson(step);
        this.conditionsBox.setValue(conditions == null || conditions.isBlank() ? "{}" : conditions);
        this.suppressDirty = false;
    }

    private String extractConditionsJson(QuestStep step) {
        HolderLookup.Provider registries = clientRegistries();
        var ops = registries != null ? RegistryOps.create(JsonOps.INSTANCE, registries) : JsonOps.INSTANCE;
        return Criterion.CODEC.encodeStart(ops, step.criterion()).result().map(encoded -> {
            if (encoded.isJsonObject()) {
                JsonObject obj = encoded.getAsJsonObject();
                if (obj.has("conditions")) {
                    return COMPACT_JSON.toJson(obj.get("conditions"));
                }
            }
            return "{}";
        }).orElse("{}");
    }

    private HolderLookup.Provider clientRegistries() {
        Minecraft minecraft = this.minecraft != null ? this.minecraft : Minecraft.getInstance();
        if (minecraft != null && minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft != null && minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
    }

    private void applyTriggerExample(ResourceLocation triggerId) {
        if (this.conditionsBox == null) {
            return;
        }
        this.conditionsBox.setValue(CriterionConditionTemplates.exampleFor(triggerId));
        setStatus(Component.translatable("screen.rpgmechanics.template_applied"), 0x55FF55);
    }

    private Optional<Criterion<?>> buildCriterionFromEditor() {
        ResourceLocation triggerId = this.triggerDropdown.getValue();
        if (triggerId == null) {
            setStatus(Component.translatable("screen.rpgmechanics.bad_criterion"), 0xFF5555);
            return Optional.empty();
        }
        JsonObject root = new JsonObject();
        root.addProperty("trigger", triggerId.toString());
        try {
            JsonElement conditions = JsonParser.parseString(this.conditionsBox.getValue().isBlank() ? "{}" : this.conditionsBox.getValue());
            root.add("conditions", conditions);
        } catch (Exception exception) {
            setStatus(Component.translatable("screen.rpgmechanics.bad_conditions_json"), 0xFF5555);
            return Optional.empty();
        }
        var ops = clientRegistries() != null
                ? RegistryOps.create(JsonOps.INSTANCE, clientRegistries())
                : JsonOps.INSTANCE;
        var parsed = Criterion.CODEC.parse(ops, root).resultOrPartial(err ->
                setStatus(Component.literal(err), 0xFF5555));
        if (parsed.isEmpty()) {
            setStatus(Component.translatable("screen.rpgmechanics.bad_criterion"), 0xFF5555);
        }
        return parsed;
    }

    /**
     * Commits the step form into {@link #editingSteps}. Returns false and sets status on failure.
     */
    private boolean commitCurrentStep() {
        if (this.stepIdBox == null) {
            return true;
        }
        String stepId = this.stepIdBox.getValue().trim();
        String stepTitle = this.stepTitleBox.getValue().trim();
        if (stepId.isEmpty() || stepTitle.isEmpty()) {
            setStatus(Component.translatable("screen.rpgmechanics.step_fields_required"), 0xFF5555);
            return false;
        }
        int ignoreIndex = this.draftingNewStep ? -1 : this.selectedStepIndex;
        if (isStepIdTaken(stepId, ignoreIndex)) {
            setStatus(Component.translatable("screen.rpgmechanics.duplicate_step_id"), 0xFF5555);
            return false;
        }
        Optional<Criterion<?>> criterion = buildCriterionFromEditor();
        if (criterion.isEmpty()) {
            return false;
        }
        QuestStep step = new QuestStep(stepId, stepTitle, this.stepDescriptionBox.getValue().trim(), criterion.get());
        String conditionsJson = this.conditionsBox.getValue().isBlank() ? "{}" : this.conditionsBox.getValue().trim();
        if (!this.draftingNewStep && this.selectedStepIndex >= 0 && this.selectedStepIndex < this.editingSteps.size()) {
            this.editingSteps.set(this.selectedStepIndex, step);
            this.editingStepConditions.set(this.selectedStepIndex, conditionsJson);
        } else {
            this.editingSteps.add(step);
            this.editingStepConditions.add(conditionsJson);
            this.selectedStepIndex = this.editingSteps.size() - 1;
            this.draftingNewStep = false;
        }
        if (this.stepList != null) {
            this.stepList.reload();
            this.stepList.selectIndex(this.selectedStepIndex);
        }
        return true;
    }

    private void removeSelectedStep() {
        if (this.selectedStepIndex < 0 || this.selectedStepIndex >= this.editingSteps.size()) {
            setStatus(Component.translatable("screen.rpgmechanics.select_step_first"), 0xFF5555);
            return;
        }
        this.editingSteps.remove(this.selectedStepIndex);
        if (this.selectedStepIndex < this.editingStepConditions.size()) {
            this.editingStepConditions.remove(this.selectedStepIndex);
        }
        markDirty();
        if (this.editingSteps.isEmpty()) {
            this.selectedStepIndex = -1;
            beginNewStep();
        } else {
            if (this.selectedStepIndex >= this.editingSteps.size()) {
                this.selectedStepIndex = this.editingSteps.size() - 1;
            }
            this.draftingNewStep = false;
            loadStepIntoForm(this.selectedStepIndex);
        }
        this.stepList.reload();
        if (this.selectedStepIndex >= 0) {
            this.stepList.selectIndex(this.selectedStepIndex);
        }
        setStatus(Component.translatable("screen.rpgmechanics.step_removed"), 0x55FF55);
    }

    private void moveSelectedStep(int delta) {
        if (this.selectedStepIndex < 0 || this.selectedStepIndex >= this.editingSteps.size()) {
            return;
        }
        int target = this.selectedStepIndex + delta;
        if (target < 0 || target >= this.editingSteps.size()) {
            return;
        }
        Collections.swap(this.editingSteps, this.selectedStepIndex, target);
        if (this.selectedStepIndex < this.editingStepConditions.size() && target < this.editingStepConditions.size()) {
            Collections.swap(this.editingStepConditions, this.selectedStepIndex, target);
        }
        this.selectedStepIndex = target;
        markDirty();
        this.stepList.reload();
        this.stepList.selectIndex(this.selectedStepIndex);
    }

    private void save() {
        if (this.iconBox != null) {
            this.editingIconValue = this.iconBox.getValue();
            this.editingIdValue = this.idBox.getValue();
            this.editingTitleValue = this.titleBox.getValue();
            this.editingDescriptionValue = this.descriptionBox.getValue();
        }

        // Save commits the current step form first (Apply is merged into Save).
        if (!commitCurrentStep()) {
            return;
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(this.editingIdValue.trim());
        } catch (Exception exception) {
            setStatus(Component.translatable("screen.rpgmechanics.bad_quest_id"), 0xFF5555);
            return;
        }
        if (this.editingTitleValue.isBlank() || this.editingSteps.isEmpty()) {
            setStatus(Component.translatable("screen.rpgmechanics.save_requires_steps"), 0xFF5555);
            return;
        }
        Optional<ResourceLocation> icon = Optional.empty();
        String iconText = this.editingIconValue.trim();
        if (!iconText.isEmpty()) {
            try {
                icon = Optional.of(ResourceLocation.parse(iconText));
            } catch (Exception exception) {
                setStatus(Component.translatable("screen.rpgmechanics.bad_icon"), 0xFF5555);
                return;
            }
        }
        QuestDefinition definition = new QuestDefinition(
                id,
                this.editingTitleValue.trim(),
                this.editingDescriptionValue.trim(),
                icon,
                this.editingRepeatable,
                List.copyOf(this.editingSteps)
        );
        PacketDistributor.sendToServer(new EditorSaveQuestPayload(definition));
        this.editingId = id;
        this.creatingNew = false;
        this.dirty = false;
        if (this.idBox != null) {
            this.idBox.setEditable(false);
        }
        this.listSelection = id;
        setStatus(Component.translatable("screen.rpgmechanics.save_sent"), 0x55FF55);
    }

    private void deleteCurrent() {
        ResourceLocation id = this.editingId;
        if (id == null) {
            try {
                id = ResourceLocation.parse(this.editingIdValue.trim());
            } catch (Exception exception) {
                setStatus(Component.translatable("screen.rpgmechanics.bad_quest_id"), 0xFF5555);
                return;
            }
        }
        PacketDistributor.sendToServer(new EditorDeleteQuestPayload(id));
        setStatus(Component.translatable("screen.rpgmechanics.delete_sent"), 0x55FF55);
        backToList();
    }

    private void deleteFromList() {
        if (this.listSelection == null) {
            setStatus(Component.translatable("screen.rpgmechanics.select_quest_first"), 0xFF5555);
            return;
        }
        PacketDistributor.sendToServer(new EditorDeleteQuestPayload(this.listSelection));
        this.listSelection = null;
        setStatus(Component.translatable("screen.rpgmechanics.delete_sent"), 0x55FF55);
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, 0xFF101010);

        if (this.mode == Mode.EDIT && this.iconBrowserOpen) {
            graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.icon_browser"), this.panelLeft + 12, this.panelTop + 8, 0xFFFF55, false);
            // Hide conditions/form bleed: only modal widgets exist.
            if (this.conditionsBox != null) {
                this.conditionsBox.visible = false;
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        } else {
            if (this.conditionsBox != null) {
                boolean hideConditions = this.triggerDropdown != null && this.triggerDropdown.isExpanded();
                this.conditionsBox.visible = !hideConditions;
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            if (this.mode == Mode.LIST) {
                renderListMode(graphics);
            } else {
                renderEditFormLabels(graphics);
            }
            if (this.triggerDropdown != null && this.triggerDropdown.isExpanded()) {
                this.triggerDropdown.renderPopup(graphics, mouseX, mouseY);
            }
        }

        if (!this.statusMessage.getString().isEmpty() && !this.iconBrowserOpen) {
            String clipped = this.font.plainSubstrByWidth(this.statusMessage.getString(), 150);
            graphics.drawString(this.font, clipped, this.panelLeft + 8, this.panelTop + PANEL_HEIGHT - 40, this.statusColor, false);
        }
    }

    private void renderListMode(GuiGraphics graphics) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.quest_list"), this.panelLeft + 8, this.panelTop + 18, 0xA0A0A0, false);

        int previewLeft = this.panelLeft + 170;
        int previewTop = this.panelTop + 28;
        graphics.fill(previewLeft, previewTop, this.panelLeft + PANEL_WIDTH - 8, this.panelTop + PANEL_HEIGHT - 36, 0xFF1A1A1A);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.preview"), previewLeft + 8, previewTop + 8, 0xFFFF55, false);

        if (this.listSelection == null) {
            graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.select_or_create"), previewLeft + 8, previewTop + 28, 0xA0A0A0, false);
            return;
        }
        QuestDefinition quest = ClientQuestCache.get(this.listSelection).orElse(null);
        if (quest == null) {
            graphics.drawString(this.font, this.listSelection.toString(), previewLeft + 8, previewTop + 28, 0xFF5555, false);
            return;
        }
        int y = previewTop + 28;
        graphics.drawString(this.font, quest.title(), previewLeft + 8, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, quest.id().toString(), previewLeft + 8, y, 0x808080, false);
        y += 14;
        for (var line : this.font.split(Component.literal(quest.description()), PANEL_WIDTH - 200)) {
            graphics.drawString(this.font, line, previewLeft + 8, y, 0xCFCFCF, false);
            y += 10;
        }
        y += 8;
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.steps_count", quest.steps().size()), previewLeft + 8, y, 0xFFFF55, false);
    }

    private void renderEditFormLabels(GuiGraphics graphics) {
        String heading = this.creatingNew
                ? Component.translatable("screen.rpgmechanics.creating_quest").getString()
                : Component.translatable("screen.rpgmechanics.editing_quest", this.editingTitleValue).getString();
        graphics.drawString(this.font, this.font.plainSubstrByWidth(heading, PANEL_WIDTH - 100), this.panelLeft + 8, this.panelTop + 6, 0xFFFFFF, false);

        int right = this.panelLeft + 170;
        int headingBottom = this.panelTop + 20;
        int questTop = headingBottom + 12;
        int stepTop = this.panelTop + 158;
        int detectTop = this.panelTop + 246;

        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.steps"), this.panelLeft + 8, headingBottom, 0xA0A0A0, false);
        graphics.drawString(this.font, "Id", right, questTop, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.field_title"), right, questTop + 30, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.field_description"), right, questTop + 60, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.field_icon"), right, questTop + 90, 0xA0A0A0, false);

        if (!this.editingIconValue.isBlank()) {
            try {
                ResourceLocation icon = ResourceLocation.parse(this.editingIconValue.trim());
                graphics.renderFakeItem(QuestIcons.resolveItemStack(icon), right + 278, questTop + 100);
            } catch (Exception ignored) {
            }
        }

        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.step_details"), right, stepTop - 10, 0xFFFF55, false);
        graphics.drawString(this.font, "Id", right, stepTop, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.field_title"), right + 96, stepTop, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.field_description"), right, stepTop + 30, 0xA0A0A0, false);
        graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.trigger"), right, detectTop - 10, 0xA0A0A0, false);
        if (this.conditionsBox == null || this.conditionsBox.visible) {
            graphics.drawString(this.font, Component.translatable("screen.rpgmechanics.conditions_json"), right, detectTop + 18, 0xA0A0A0, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.triggerDropdown != null && this.triggerDropdown.isExpanded()) {
            if (this.triggerDropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            this.triggerDropdown.collapse();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.triggerDropdown != null && this.triggerDropdown.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (this.iconGrid != null && this.iconGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private class QuestBrowserList extends ObjectSelectionList<QuestBrowserList.Entry> {
        public QuestBrowserList(int width, int height, int y) {
            super(QuestEditorScreen.this.minecraft, width, height, y, 18);
            for (QuestDefinition quest : ClientQuestCache.definitions()) {
                this.addEntry(new Entry(quest));
            }
        }

        private void selectId(ResourceLocation id) {
            for (Entry entry : this.children()) {
                if (entry.quest.id().equals(id)) {
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

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final QuestDefinition quest;

            private Entry(QuestDefinition quest) {
                this.quest = quest;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean selected = QuestEditorScreen.this.listSelection != null && QuestEditorScreen.this.listSelection.equals(this.quest.id());
                int color = selected ? 0xFFFF55 : (hovering ? 0xFFFFA0 : 0xFFFFFF);
                graphics.drawString(QuestEditorScreen.this.font, this.quest.title(), left + 2, top + 4, color, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                QuestEditorScreen.this.listSelection = this.quest.id();
                QuestBrowserList.this.setSelected(this);
                if (button == 0 && Screen.hasShiftDown()) {
                    QuestEditorScreen.this.openEdit(this.quest);
                }
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.quest.title());
            }
        }
    }

    private class StepList extends ObjectSelectionList<StepList.Entry> {
        public StepList(int width, int height, int y) {
            super(QuestEditorScreen.this.minecraft, width, height, y, 18);
            reload();
        }

        private void reload() {
            this.clearEntries();
            for (int i = 0; i < QuestEditorScreen.this.editingSteps.size(); i++) {
                this.addEntry(new Entry(i, QuestEditorScreen.this.editingSteps.get(i)));
            }
        }

        private void selectIndex(int index) {
            if (index < 0 || index >= this.children().size()) {
                this.setSelected(null);
                return;
            }
            this.setSelected(this.children().get(index));
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final int index;
            private final QuestStep step;

            private Entry(int index, QuestStep step) {
                this.index = index;
                this.step = step;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean selected = QuestEditorScreen.this.selectedStepIndex == this.index && !QuestEditorScreen.this.draftingNewStep;
                int color = selected ? 0xFFFF55 : (hovering ? 0xFFFFA0 : 0xFFFFFF);
                String label = (this.index + 1) + ". " + this.step.title();
                graphics.drawString(
                        QuestEditorScreen.this.font,
                        QuestEditorScreen.this.font.plainSubstrByWidth(label, width - 6),
                        left + 2,
                        top + 5,
                        color,
                        false
                );
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                QuestEditorScreen.this.selectedStepIndex = this.index;
                QuestEditorScreen.this.draftingNewStep = false;
                QuestEditorScreen.this.loadStepIntoForm(this.index);
                StepList.this.setSelected(this);
                QuestEditorScreen.this.setStatus(Component.translatable("screen.rpgmechanics.editing_step"), 0xA0A0A0);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.step.title());
            }
        }
    }
}
