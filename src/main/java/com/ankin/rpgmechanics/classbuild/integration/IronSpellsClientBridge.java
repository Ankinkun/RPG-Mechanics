package com.ankin.rpgmechanics.classbuild.integration;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * CompileOnly Iron Spells client helpers for the combat HUD.
 */
public final class IronSpellsClientBridge {
    private IronSpellsClientBridge() {
    }

    public static int getMana() {
        return ClientMagicData.getPlayerMana();
    }

    public static int getMaxMana(Player player) {
        AttributeInstance attr = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (attr == null) {
            return 100;
        }
        return Math.max(1, (int) attr.getValue());
    }

    public static float getCooldownPercent(ResourceLocation spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return 0f;
        }
        return ClientMagicData.getCooldownPercent(spell);
    }

    public static ResourceLocation getSpellIcon(ResourceLocation spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return null;
        }
        return spell.getSpellIconResource();
    }
}
