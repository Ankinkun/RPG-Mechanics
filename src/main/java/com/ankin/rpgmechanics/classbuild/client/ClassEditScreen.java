package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;

import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTab;
import com.ankin.rpgmechanics.classbuild.client.ui.RpgHubTabBar;
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
 * Edit Class hub tab. Role/track locked to Damage Dealer / Darkness.
 */
public class ClassEditScreen extends Screen {
    private ResourceLocation melee;
    private ResourceLocation movement;
    private ResourceLocation ranged;
    private ResourceLocation ultimate;

    public ClassEditScreen() {
        super(Component.translatable("screen.rpgmechanics.edit_class"));
        ClassBuildState cached = ClassBuildClientPayloadHandlers.cached();
        this.melee = cached.melee();
        this.movement = cached.movement();
        this.ranged = cached.ranged();
        this.ultimate = cached.ultimate();
    }

    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ClassEditScreen());
    }

    @Override
    protected void init() {
        this.clearWidgets();
        RpgHubTabBar.addTo(this::addRenderableWidget, this.width, RpgHubTab.CLASS);
        int y = RpgUiTheme.TAB_BAR_H + 24;
        y = addAbilityRow(y, "screen.rpgmechanics.ability.melee", ClassBuildCatalog.MELEE, melee, id -> melee = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.movement", ClassBuildCatalog.MOVEMENT, movement, id -> movement = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.ranged", ClassBuildCatalog.RANGED, ranged, id -> ranged = id);
        y = addAbilityRow(y + 8, "screen.rpgmechanics.ability.ultimate", ClassBuildCatalog.ULTIMATE, ultimate, id -> ultimate = id);

        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.rpgmechanics.apply_class"), b -> {
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
        }).bounds(cx - 60, Math.min(y + 16, this.height - 28), 120, 20).build());
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
        RpgUiPanels.drawTabBarBg(graphics, this.width);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.darkness_dd"),
                this.width / 2,
                RpgUiTheme.TAB_BAR_H + 8,
                RpgUiTheme.ACCENT
        );
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
