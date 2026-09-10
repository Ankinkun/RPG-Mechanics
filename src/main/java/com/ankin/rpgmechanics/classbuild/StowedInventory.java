package com.ankin.rpgmechanics.classbuild;

import java.util.ArrayList;
import java.util.List;

import com.ankin.rpgmechanics.classbuild.integration.EpicFightSoft;
import com.ankin.rpgmechanics.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Server-authoritative bag: stowed list is the only backpack.
 * Vanilla hotbar 1–8 / storage 9–35 are not used; equipped gear mirrors onto armor + hotbar 0 + offhand.
 */
public final class StowedInventory {
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    private StowedInventory() {
    }

    public static boolean isApplyingEquipment() {
        return Boolean.TRUE.equals(APPLYING.get());
    }

    public static void runApplying(Runnable action) {
        APPLYING.set(true);
        try {
            action.run();
        } finally {
            APPLYING.set(false);
        }
    }

    public static GearSlot resolveTarget(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        for (GearSlot slot : List.of(GearSlot.HELMET, GearSlot.CHEST, GearSlot.LEGS, GearSlot.BOOTS)) {
            if (stack.canEquip(slot.equipmentSlot(), player)) {
                return slot;
            }
        }
        if (stack.getItem() instanceof ShieldItem) {
            return GearSlot.OFFHAND;
        }
        return GearSlot.WEAPON;
    }

    /**
     * Deposit into the active character's stowed bag (merge stacks when possible).
     * Shrinks {@code stack} to empty when fully accepted. Never touches hotbar/hand.
     */
    public static boolean stowPickup(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return true;
        }
        if (isApplyingEquipment()) {
            return false;
        }
        CharacterRoster roster = ClassBuildManager.getRoster(player);
        if (!roster.hasActive()) {
            return false;
        }
        int active = roster.activeSlot();
        CharacterSlot slot = roster.slot(active);
        List<ItemStack> stowed = new ArrayList<>(slot.stowed());

        // Merge into existing stacks first.
        for (ItemStack existing : stowed) {
            if (stack.isEmpty()) {
                break;
            }
            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, stack.getCount());
            existing.grow(move);
            stack.shrink(move);
        }
        while (!stack.isEmpty()) {
            int take = Math.min(stack.getMaxStackSize(), stack.getCount());
            ItemStack piece = stack.split(take);
            stowed.add(piece);
        }

        CharacterSlot updated = slot.withStowed(stowed);
        // Write without going through setRoster→sync only once
        player.setData(ModAttachments.PLAYER_CHARACTER_ROSTER, roster.withSlot(active, updated));
        ClassBuildManager.sync(player);
        return true;
    }

    public static String equipFromStowed(ServerPlayer player, int stowedIndex) {
        CharacterRoster roster = ClassBuildManager.getRoster(player);
        if (!roster.hasActive()) {
            return "No active character";
        }
        int active = roster.activeSlot();
        CharacterSlot slot = roster.slot(active);
        List<ItemStack> stowed = new ArrayList<>(slot.stowed());
        if (stowedIndex < 0 || stowedIndex >= stowed.size()) {
            return "Invalid bag index";
        }
        ItemStack toEquip = stowed.remove(stowedIndex);
        GearSlot target = resolveTarget(player, toEquip);
        if (target == null) {
            stowed.add(stowedIndex, toEquip);
            return "Cannot equip item";
        }

        ItemStack previous = slot.get(target);
        if (!previous.isEmpty()) {
            stowed.add(previous.copy());
        }
        slot = slot.withGear(target, toEquip);

        if (target == GearSlot.WEAPON && EpicFightSoft.isTwoHanded(player, toEquip)) {
            ItemStack off = slot.offhand();
            if (!off.isEmpty()) {
                stowed.add(off.copy());
                slot = slot.withGear(GearSlot.OFFHAND, ItemStack.EMPTY);
            }
        }

        slot = slot.withStowed(stowed);
        ClassBuildManager.setRoster(player, roster.withSlot(active, slot));
        ClassBuildManager.restoreEquipmentToPlayer(player);
        return null;
    }

    public static String unequip(ServerPlayer player, GearSlot gearSlot) {
        if (gearSlot == null) {
            return "Invalid slot";
        }
        CharacterRoster roster = ClassBuildManager.getRoster(player);
        if (!roster.hasActive()) {
            return "No active character";
        }
        int active = roster.activeSlot();
        CharacterSlot slot = roster.slot(active);
        ItemStack current = slot.get(gearSlot);
        if (current.isEmpty()) {
            return null;
        }
        List<ItemStack> stowed = new ArrayList<>(slot.stowed());
        stowed.add(current.copy());
        slot = slot.withGear(gearSlot, ItemStack.EMPTY).withStowed(stowed);
        ClassBuildManager.setRoster(player, roster.withSlot(active, slot));
        ClassBuildManager.restoreEquipmentToPlayer(player);
        return null;
    }

    /** Sweep hotbar 1–8 + storage 9–35 into stowed and clear them. */
    public static CharacterSlot migrateInventoryLeftovers(ServerPlayer player, CharacterSlot slot) {
        Inventory inv = player.getInventory();
        List<ItemStack> stowed = new ArrayList<>(slot.stowed());
        boolean changed = false;
        for (int i = 1; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                stowed.add(stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed ? slot.withStowed(stowed) : slot;
    }

    /** Fast path used every tick: migrate leftovers and sync if anything moved. */
    public static void scrubVanillaSlots(ServerPlayer player) {
        if (player == null || player.isCreative() || isApplyingEquipment()) {
            return;
        }
        CharacterRoster roster = ClassBuildManager.getRoster(player);
        if (!roster.hasActive()) {
            return;
        }
        int active = roster.activeSlot();
        CharacterSlot before = roster.slot(active);
        CharacterSlot after = migrateInventoryLeftovers(player, before);
        if (after != before) {
            player.setData(ModAttachments.PLAYER_CHARACTER_ROSTER, roster.withSlot(active, after));
            ClassBuildManager.sync(player);
        }
        // Keep selected on weapon slot.
        player.getInventory().selected = 0;
    }
}
