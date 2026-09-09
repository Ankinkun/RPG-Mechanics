package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;

import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.network.ConfirmClassBuildPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Edit Class from equipment sheet. Role/track locked to Damage Dealer / Darkness.
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
        int y = 48;
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
            PacketDistributor.sendToServer(new ConfirmClassBuildPayload(draft));
            this.onClose();
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
            this.addRenderableWidget(Button.builder(Component.literal(on ? "[" + name + "]" : name), b -> {
                setter.accept(option);
                rebuildWidgets();
            }).bounds(x, y, 70, 20).build());
            x += 74;
        }
        return y + 24;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.rpgmechanics.darkness_dd"),
                this.width / 2,
                28,
                0xC080FF
        );
    }
}
