package com.ankin.rpgmechanics.quest.client;

import java.util.List;

import com.ankin.rpgmechanics.quest.QuestProgressEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Advancement-styled toast for quest initiation, step completion, and finish.
 */
public final class QuestToastHelper {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");

    private QuestToastHelper() {
    }

    public static void show(QuestProgressEvent event, String questTitle, String stepTitle, ResourceLocation icon) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        Component header = headerFor(event);
        Component title = Component.literal(stepTitle != null && !stepTitle.isBlank() ? stepTitle : questTitle);
        boolean challenge = event == QuestProgressEvent.COMPLETED;
        ItemStack stack = QuestIcons.resolveItemStack(icon);
        boolean textureIcon = icon != null
                && icon.getNamespace().equals("rpgmechanics")
                && icon.getPath().startsWith("quest_icon/");

        if (textureIcon) {
            QuestIcons.ensureCustomIconsLoaded();
            minecraft.getToasts().addToast(new QuestToast(header, title, null, icon, challenge));
        } else {
            minecraft.getToasts().addToast(new QuestToast(header, title, stack, null, challenge));
        }
    }

    private static Component headerFor(QuestProgressEvent event) {
        return switch (event) {
            case INITIATED -> Component.translatable("toast.rpgmechanics.quest_started");
            case COMPLETED -> Component.translatable("toast.rpgmechanics.quest_complete");
            default -> Component.translatable("toast.rpgmechanics.step_complete");
        };
    }

    private static final class QuestToast implements Toast {
        private final Component header;
        private final Component title;
        private final ItemStack itemIcon;
        private final ResourceLocation textureIcon;
        private final boolean challengeStyle;
        private boolean playedSound;

        private QuestToast(Component header, Component title, ItemStack itemIcon, ResourceLocation textureIcon, boolean challengeStyle) {
            this.header = header;
            this.title = title;
            this.itemIcon = itemIcon;
            this.textureIcon = textureIcon;
            this.challengeStyle = challengeStyle;
        }

        @Override
        public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
            graphics.blitSprite(BACKGROUND, 0, 0, this.width(), this.height());
            Minecraft minecraft = toastComponent.getMinecraft();
            List<FormattedCharSequence> lines = minecraft.font.split(this.title, 125);
            int headerColor = (this.challengeStyle ? 0xFF88FF : 0xFFFF00) | 0xFF000000;

            if (lines.size() == 1) {
                graphics.drawString(minecraft.font, this.header, 30, 7, headerColor, false);
                graphics.drawString(minecraft.font, lines.getFirst(), 30, 18, 0xFFFFFF, false);
            } else {
                int first = Math.min(lines.size() - 1, Math.max(0, (int) ((timeSinceLastVisible - 1500L) / 1500L)));
                if (timeSinceLastVisible < 1500L) {
                    graphics.drawString(minecraft.font, this.header, 30, 11, headerColor, false);
                } else {
                    graphics.drawString(minecraft.font, lines.get(first), 30, 11, 0xFFFFFF, false);
                }
            }

            if (this.textureIcon != null) {
                graphics.blit(this.textureIcon, 8, 8, 0, 0, 16, 16, 16, 16);
            } else if (this.itemIcon != null) {
                graphics.renderFakeItem(this.itemIcon, 8, 8);
            }

            if (this.challengeStyle && !this.playedSound && timeSinceLastVisible > 0L) {
                this.playedSound = true;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }

            return timeSinceLastVisible >= AdvancementToast.DISPLAY_TIME * toastComponent.getNotificationDisplayTimeMultiplier()
                    ? Visibility.HIDE
                    : Visibility.SHOW;
        }
    }
}
