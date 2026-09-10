package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;

import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiPanels;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgUiTheme;
import com.ankin.rpgmechanics.classbuild.network.ConfirmClassBuildPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Role + kit picker for creating a character in a specific roster slot.
 */
public class ClassSelectScreen extends Screen {
    private enum Phase {
        ROLE,
        KIT
    }

    private final int slotIndex;
    private final boolean titleFlow;
    private Phase phase = Phase.ROLE;
    private ResourceLocation melee = ClassBuildCatalog.DEFAULT_MELEE;
    private ResourceLocation movement = ClassBuildCatalog.DEFAULT_MOVEMENT;
    private ResourceLocation ranged = ClassBuildCatalog.DEFAULT_RANGED;
    private ResourceLocation ultimate = ClassBuildCatalog.DEFAULT_ULTIMATE;

    public ClassSelectScreen(int slotIndex) {
        this(slotIndex, false);
    }

    public ClassSelectScreen(int slotIndex, boolean titleFlow) {
        super(Component.translatable("screen.rpgmechanics.class_select"));
        this.slotIndex = slotIndex;
        this.titleFlow = titleFlow;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (phase == Phase.ROLE) {
            initRolePhase();
        } else {
            initKitPhase();
        }
    }

    private void initRolePhase() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.rpgmechanics.role.tank")
                        .append(Component.literal(" — "))
                        .append(Component.translatable("screen.rpgmechanics.coming_soon")),
                b -> {
                }
        ).bounds(cx - 100, y, 200, 20).build()).active = false;

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.rpgmechanics.role.damage_dealer"),
                b -> {
                    phase = Phase.KIT;
                    rebuildWidgets();
                }
        ).bounds(cx - 100, y + 28, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.rpgmechanics.role.support")
                        .append(Component.literal(" — "))
                        .append(Component.translatable("screen.rpgmechanics.coming_soon")),
                b -> {
                }
        ).bounds(cx - 100, y + 56, 200, 20).build()).active = false;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            this.minecraft.setScreen(new CharacterSelectScreen(titleFlow));
        }).bounds(cx - 100, y + 92, 200, 20).build());
    }

    private void initKitPhase() {
        int cx = this.width / 2;
        int y = 48;
        y = addAbilityRow(y, "screen.rpgmechanics.ability.melee", ClassBuildCatalog.MELEE, melee, id -> melee = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.movement", ClassBuildCatalog.MOVEMENT, movement, id -> movement = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.ranged", ClassBuildCatalog.RANGED, ranged, id -> ranged = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.ultimate", ClassBuildCatalog.ULTIMATE, ultimate, id -> ultimate = id);

        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.confirm_class"), b -> {
            ClassBuildState draft = new ClassBuildState(
                    false,
                    ClassBuildState.ClassRole.DAMAGE_DEALER,
                    ClassBuildState.ElementTrack.DARKNESS,
                    melee,
                    movement,
                    ranged,
                    ultimate
            );
            if (titleFlow) {
                TitleSelectPending.playNew(slotIndex, draft);
            } else {
                PacketDistributor.sendToServer(new ConfirmClassBuildPayload(slotIndex, draft));
                this.onClose();
            }
        }).bounds(cx - 60, Math.min(y + 16, this.height - 28), 120, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            phase = Phase.ROLE;
            rebuildWidgets();
        }).bounds(cx - 60, Math.min(y + 40, this.height - 28), 120, 20).build());
    }

    private int addAbilityRow(
            int y,
            String labelKey,
            List<ResourceLocation> options,
            ResourceLocation selected,
            java.util.function.Consumer<ResourceLocation> setter
    ) {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(labelKey), b -> {
        }).bounds(cx - 150, y, 80, 20).build()).active = false;

        int x = cx - 60;
        for (ResourceLocation option : options) {
            boolean on = option.equals(selected);
            String name = ClassBuildCatalog.displayName(option);
            Button button = Button.builder(Component.literal(on ? "[" + name + "]" : name), b -> {
                setter.accept(option);
                rebuildWidgets();
            }).bounds(x, y, 70, 20).build();
            this.addRenderableWidget(button);
            x += 74;
        }
        return y + 24;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RpgUiPanels.drawFullDim(graphics, this.width, this.height);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, RpgUiTheme.TEXT);
        if (phase == Phase.KIT) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.rpgmechanics.darkness_dd"),
                    this.width / 2,
                    32,
                    RpgUiTheme.ACCENT
            );
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
