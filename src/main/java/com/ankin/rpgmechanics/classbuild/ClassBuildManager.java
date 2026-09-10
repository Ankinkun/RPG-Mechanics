package com.ankin.rpgmechanics.classbuild;

import com.ankin.rpgmechanics.classbuild.integration.EpicFightSoft;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;
import com.ankin.rpgmechanics.classbuild.network.OpenClassSelectPayload;
import com.ankin.rpgmechanics.classbuild.network.SyncClassBuildPayload;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.ankin.rpgmechanics.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClassBuildManager {
    private ClassBuildManager() {
    }

    public static boolean isFeatureEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildEnabled.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static int spellLevel() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return 3;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildSpellLevel.get();
        } catch (IllegalStateException exception) {
            return 3;
        }
    }

    public static boolean forceAdventure() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildForceAdventure.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static CharacterRoster getRoster(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_CHARACTER_ROSTER);
    }

    public static void setRoster(ServerPlayer player, CharacterRoster roster) {
        player.setData(ModAttachments.PLAYER_CHARACTER_ROSTER, roster);
        sync(player);
    }

    /** Active kit, or EMPTY when no character is selected. */
    public static ClassBuildState get(ServerPlayer player) {
        return getRoster(player).activeBuildOrEmpty();
    }

    public static boolean hasActiveCharacter(ServerPlayer player) {
        return getRoster(player).hasActive();
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncClassBuildPayload(getRoster(player)));
    }

    public static void openSelectIfNeeded(ServerPlayer player) {
        if (!isFeatureEnabled()) {
            return;
        }
        if (!hasActiveCharacter(player)) {
            PacketDistributor.sendToPlayer(player, new OpenClassSelectPayload());
        }
    }

    public static void openCharacterSelect(ServerPlayer player) {
        if (!isFeatureEnabled()) {
            return;
        }
        saveEquipmentFromPlayer(player);
        setRoster(player, getRoster(player).clearActive());
        PacketDistributor.sendToPlayer(player, new OpenClassSelectPayload());
    }

    /**
     * Create or overwrite a slot from a kit draft, then activate it.
     */
    public static String confirm(ServerPlayer player, int slotIndex, ClassBuildState draft) {
        if (!isFeatureEnabled()) {
            return "Class buildcrafting is disabled";
        }
        if (slotIndex < 0 || slotIndex >= CharacterRoster.SLOT_COUNT) {
            return "Invalid character slot";
        }
        ClassBuildState next = new ClassBuildState(
                true,
                ClassBuildState.ClassRole.DAMAGE_DEALER,
                ClassBuildState.ElementTrack.DARKNESS,
                draft.melee(),
                draft.movement(),
                draft.ranged(),
                draft.ultimate()
        );
        var error = next.validationError();
        if (error.isPresent()) {
            return error.get();
        }

        CharacterRoster roster = getRoster(player);
        if (roster.hasActive()) {
            saveEquipmentFromPlayer(player);
            roster = getRoster(player);
        }

        CharacterSlot existing = roster.slot(slotIndex);
        CharacterSlot created = existing.occupied()
                ? existing.withBuild(next)
                : CharacterSlot.create(draftName(next), next);
        roster = roster.withSlot(slotIndex, created).withActive(slotIndex);
        setRoster(player, roster);
        restoreEquipmentToPlayer(player);
        applyLoadout(player);
        if (forceAdventure()) {
            player.setGameMode(GameType.ADVENTURE);
        }
        return null;
    }

    /** Update the active character's kit (Edit Class). */
    public static String applyActiveKit(ServerPlayer player, ClassBuildState draft) {
        CharacterRoster roster = getRoster(player);
        if (!roster.hasActive()) {
            return "No active character";
        }
        return confirm(player, roster.activeSlot(), draft);
    }

    public static String selectSlot(ServerPlayer player, int slotIndex) {
        if (!isFeatureEnabled()) {
            return "Class buildcrafting is disabled";
        }
        if (slotIndex < 0 || slotIndex >= CharacterRoster.SLOT_COUNT) {
            return "Invalid character slot";
        }
        CharacterRoster roster = getRoster(player);
        if (!roster.slot(slotIndex).occupied()) {
            return "Character slot is empty";
        }
        if (roster.hasActive()) {
            saveEquipmentFromPlayer(player);
            roster = getRoster(player);
        }
        setRoster(player, roster.withActive(slotIndex));
        restoreEquipmentToPlayer(player);
        applyLoadout(player);
        if (forceAdventure()) {
            player.setGameMode(GameType.ADVENTURE);
        }
        return null;
    }

    public static String deleteSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= CharacterRoster.SLOT_COUNT) {
            return "Invalid character slot";
        }
        CharacterRoster roster = getRoster(player);
        if (roster.activeSlot() == slotIndex) {
            saveEquipmentFromPlayer(player);
            roster = getRoster(player).clearActive();
        }
        roster = roster.withSlot(slotIndex, CharacterSlot.EMPTY);
        if (roster.activeSlot() == slotIndex) {
            roster = roster.clearActive();
        }
        setRoster(player, roster);
        if (!roster.hasActive()) {
            openSelectIfNeeded(player);
        }
        return null;
    }

    public static void applyLoadout(ServerPlayer player) {
        ClassBuildState state = get(player);
        if (!state.confirmed()) {
            return;
        }
        IronSpellsSoft.apply(player, state, spellLevel());
        EpicFightSoft.grantStarterSkills(player);
        EpicFightSoft.ensureCombatMode(player);
    }

    public static void reconcile(ServerPlayer player) {
        ClassBuildState state = get(player);
        if (!state.confirmed()) {
            return;
        }
        IronSpellsSoft.reconcile(player, state, spellLevel());
        saveEquipmentFromPlayer(player);
    }

    public static void reset(ServerPlayer player) {
        setRoster(player, CharacterRoster.EMPTY);
        openSelectIfNeeded(player);
    }

    public static void saveEquipmentFromPlayer(ServerPlayer player) {
        CharacterRoster roster = getRoster(player);
        if (!roster.hasActive()) {
            return;
        }
        Inventory inv = player.getInventory();
        CharacterSlot updated = roster.slot(roster.activeSlot()).withEquipment(
                player.getItemBySlot(EquipmentSlot.HEAD).copy(),
                player.getItemBySlot(EquipmentSlot.CHEST).copy(),
                player.getItemBySlot(EquipmentSlot.LEGS).copy(),
                player.getItemBySlot(EquipmentSlot.FEET).copy(),
                inv.getItem(0).copy(),
                inv.offhand.getFirst().copy()
        );
        player.setData(ModAttachments.PLAYER_CHARACTER_ROSTER, roster.withSlot(roster.activeSlot(), updated));
    }

    public static void restoreEquipmentToPlayer(ServerPlayer player) {
        CharacterRoster roster = getRoster(player);
        if (!roster.hasActive()) {
            return;
        }
        int active = roster.activeSlot();
        CharacterSlot slot = StowedInventory.migrateInventoryLeftovers(player, roster.slot(active));
        if (slot != roster.slot(active)) {
            roster = roster.withSlot(active, slot);
            setRoster(player, roster);
            roster = getRoster(player);
            slot = roster.slot(active);
        }
        CharacterSlot equipped = slot;
        StowedInventory.runApplying(() -> {
            Inventory inv = player.getInventory();
            player.setItemSlot(EquipmentSlot.HEAD, equipped.helmet().copy());
            player.setItemSlot(EquipmentSlot.CHEST, equipped.chest().copy());
            player.setItemSlot(EquipmentSlot.LEGS, equipped.legs().copy());
            player.setItemSlot(EquipmentSlot.FEET, equipped.boots().copy());
            inv.setItem(0, equipped.weapon().copy());
            inv.offhand.set(0, equipped.offhand().copy());
            inv.selected = 0;
        });
    }

    private static String draftName(ClassBuildState build) {
        return "Darkness DD";
    }
}
