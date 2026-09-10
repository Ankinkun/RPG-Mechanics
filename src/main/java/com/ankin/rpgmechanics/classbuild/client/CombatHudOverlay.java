package com.ankin.rpgmechanics.classbuild.client;

import java.util.List;
import java.util.Set;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsClientSoft;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * LoL-style bottom HUD: HP + 4 abilities with CDs + mana. Hides vanilla hotbar/XP/food/health.
 */
@EventBusSubscriber(modid = RpgMechanics.MOD_ID, value = Dist.CLIENT)
public final class CombatHudOverlay {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "combat_hud");

    private static final Set<ResourceLocation> HIDDEN_LAYERS = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            // Iron's Spells chrome — our LoL HUD owns mana / abilities
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_overlay"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_bar"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_wheel"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cast_bar"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "recast_bar")
    );

    private static final int ABILITY_SIZE = 36;
    private static final int ABILITY_GAP = 6;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_WIDTH = 120;

    private CombatHudOverlay() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, CombatHudOverlay::render);
    }

    private static boolean hudEnabled() {
        if (!RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.CLIENT.classbuildCombatHud.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    private static boolean shouldShow() {
        if (!hudEnabled() || !ClassBuildClientPayloadHandlers.hasActiveCharacter()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isCreative()) {
            return false;
        }
        return minecraft.player != null && !minecraft.options.hideGui && minecraft.screen == null;
    }

    @SubscribeEvent
    public static void onHideVanilla(RenderGuiLayerEvent.Pre event) {
        if (!shouldShow()) {
            return;
        }
        if (HIDDEN_LAYERS.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        CombatHudKeybinds.tick();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!shouldShow()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ClassBuildState build = ClassBuildClientPayloadHandlers.cached();
        List<ResourceLocation> spells = build.orderedSpells();

        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int abilitiesWidth = 4 * ABILITY_SIZE + 3 * ABILITY_GAP;
        int clusterWidth = BAR_WIDTH + 12 + abilitiesWidth + 12 + BAR_WIDTH;
        int left = (screenW - clusterWidth) / 2;
        int bottom = screenH - 28;
        int abilityY = bottom - ABILITY_SIZE;
        int barY = abilityY + (ABILITY_SIZE - BAR_HEIGHT) / 2;

        // HP
        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float absorb = player.getAbsorptionAmount();
        drawBar(graphics, left, barY, BAR_WIDTH, BAR_HEIGHT, hp / Math.max(1f, maxHp), 0xFFCC3333, 0xFF331111);
        if (absorb > 0) {
            int absorbW = (int) (BAR_WIDTH * Math.min(1f, absorb / Math.max(1f, maxHp)));
            graphics.fill(left, barY, left + absorbW, barY + 3, 0xFFE0C040);
        }
        graphics.drawCenteredString(
                minecraft.font,
                (int) hp + " / " + (int) maxHp,
                left + BAR_WIDTH / 2,
                barY - 10,
                0xFFFFFF
        );

        // Abilities
        int abilityX = left + BAR_WIDTH + 12;
        for (int i = 0; i < 4; i++) {
            int x = abilityX + i * (ABILITY_SIZE + ABILITY_GAP);
            graphics.fill(x - 1, abilityY - 1, x + ABILITY_SIZE + 1, abilityY + ABILITY_SIZE + 1, 0xFF1A1A22);
            graphics.fill(x, abilityY, x + ABILITY_SIZE, abilityY + ABILITY_SIZE, 0xFF2A2A38);

            ResourceLocation spellId = i < spells.size() ? spells.get(i) : null;
            ResourceLocation icon = spellId == null ? null : IronSpellsClientSoft.getSpellIcon(spellId);
            if (icon != null) {
                graphics.blit(icon, x + 2, abilityY + 2, 0, 0, ABILITY_SIZE - 4, ABILITY_SIZE - 4, ABILITY_SIZE - 4, ABILITY_SIZE - 4);
            }

            float cd = spellId == null ? 0f : IronSpellsClientSoft.getCooldownPercent(spellId);
            if (cd > 0f) {
                int shade = (int) (ABILITY_SIZE * cd);
                graphics.fill(x, abilityY + ABILITY_SIZE - shade, x + ABILITY_SIZE, abilityY + ABILITY_SIZE, 0xAA000000);
            }

            String hint = CombatHudKeybinds.hint(i);
            if (!hint.isEmpty()) {
                graphics.drawString(minecraft.font, hint, x + 2, abilityY + ABILITY_SIZE - 9, 0xFFE0E0E0, true);
            }
        }

        // Mana
        int manaX = abilityX + abilitiesWidth + 12;
        int mana = IronSpellsClientSoft.getMana();
        int maxMana = IronSpellsClientSoft.getMaxMana(player);
        float manaFrac = maxMana <= 0 ? 0f : Math.min(1f, mana / (float) maxMana);
        drawBar(graphics, manaX, barY, BAR_WIDTH, BAR_HEIGHT, manaFrac, 0xFF3380FF, 0xFF112244);
        graphics.drawCenteredString(
                minecraft.font,
                mana + " / " + maxMana,
                manaX + BAR_WIDTH / 2,
                barY - 10,
                0xFFFFFF
        );
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int w, int h, float fraction, int fill, int bg) {
        graphics.fill(x, y, x + w, y + h, bg);
        int fw = Math.max(0, Math.min(w, (int) (w * Math.max(0f, Math.min(1f, fraction)))));
        if (fw > 0) {
            graphics.fill(x, y, x + fw, y + h, fill);
        }
        graphics.renderOutline(x, y, w, h, 0xFF000000);
    }
}
