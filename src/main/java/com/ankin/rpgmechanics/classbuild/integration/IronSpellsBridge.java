package com.ankin.rpgmechanics.classbuild.integration;

import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildCatalog;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Soft Iron Spells + Curios bridge. Loaded reflectively via {@link IronSpellsSoft}.
 */
public final class IronSpellsBridge {
    public static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    private IronSpellsBridge() {
    }

    public static boolean isAvailable() {
        return IronSpellsSoft.isAvailable();
    }

    public static void apply(ServerPlayer player, ClassBuildState build, int spellLevel) {
        if (!isAvailable() || !build.confirmed()) {
            return;
        }
        ResourceLocation bookId = ClassBuildCatalog.bookForUltimate(build.ultimate());
        Item bookItem = BuiltInRegistries.ITEM.get(bookId);
        if (bookItem == Items.AIR) {
            RpgMechanics.LOGGER.warn("Missing spellbook item {}", bookId);
            return;
        }

        ItemStack stack = new ItemStack(bookItem);
        ISpellContainerMutable mutable = ISpellContainer.create(4, true, true).mutableCopy();
        List<ResourceLocation> spells = build.orderedSpells();
        for (int i = 0; i < spells.size(); i++) {
            AbstractSpell spell = SpellRegistry.getSpell(spells.get(i));
            if (spell == null || spell == SpellRegistry.none()) {
                RpgMechanics.LOGGER.warn("Missing spell {}", spells.get(i));
                continue;
            }
            int level = Math.max(spell.getMinLevel(), Math.min(spellLevel, spell.getMaxLevel()));
            mutable.addSpellAtIndex(spell, i, level, true);
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        IronSpellsSoft.markManaged(stack);
        APPLYING.set(true);
        try {
            Utils.setPlayerSpellbookStack(player, stack);
        } finally {
            APPLYING.set(false);
        }
    }

    public static boolean isManaged(ItemStack stack) {
        return IronSpellsSoft.isManaged(stack);
    }

    public static void markManaged(ItemStack stack) {
        IronSpellsSoft.markManaged(stack);
    }

    public static boolean matchesEquipped(ServerPlayer player, ClassBuildState build, int spellLevel) {
        if (!isAvailable() || !build.confirmed()) {
            return true;
        }
        ItemStack equipped = Utils.getPlayerSpellbookStack(player);
        if (equipped == null || equipped.isEmpty() || !isManaged(equipped)) {
            return false;
        }
        ResourceLocation expectedBook = ClassBuildCatalog.bookForUltimate(build.ultimate());
        if (!BuiltInRegistries.ITEM.getKey(equipped.getItem()).equals(expectedBook)) {
            return false;
        }
        if (!ISpellContainer.isSpellContainer(equipped)) {
            return false;
        }
        ISpellContainer container = ISpellContainer.get(equipped);
        if (container.getMaxSpellCount() != 4) {
            return false;
        }
        List<ResourceLocation> expected = build.orderedSpells();
        for (int i = 0; i < expected.size(); i++) {
            SpellData data = container.getSpellAtIndex(i);
            if (data == null || data.getSpell() == null) {
                return false;
            }
            if (!data.getSpell().getSpellResource().equals(expected.get(i))) {
                return false;
            }
            AbstractSpell spell = data.getSpell();
            int level = Math.max(spell.getMinLevel(), Math.min(spellLevel, spell.getMaxLevel()));
            if (data.getLevel() != level || !data.isLocked()) {
                return false;
            }
        }
        return true;
    }

    public static void reconcile(ServerPlayer player, ClassBuildState build, int spellLevel) {
        if (!matchesEquipped(player, build, spellLevel)) {
            apply(player, build, spellLevel);
        }
    }
}
