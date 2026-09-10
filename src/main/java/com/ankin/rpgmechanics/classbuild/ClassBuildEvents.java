package com.ankin.rpgmechanics.classbuild;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.integration.EpicFightSoft;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID)
public final class ClassBuildEvents {
    private static int reconcileTicker;

    private ClassBuildEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.isFeatureEnabled()) {
            return;
        }
        // Destiny-style: always land on character select each session.
        ClassBuildManager.saveEquipmentFromPlayer(player);
        ClassBuildManager.setRoster(player, ClassBuildManager.getRoster(player).clearActive());
        ClassBuildManager.openSelectIfNeeded(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.isFeatureEnabled()) {
            return;
        }
        ClassBuildManager.saveEquipmentFromPlayer(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.isFeatureEnabled()) {
            return;
        }
        ClassBuildManager.sync(player);
        if (ClassBuildManager.hasActiveCharacter(player)) {
            ClassBuildManager.applyLoadout(player);
        } else {
            ClassBuildManager.openSelectIfNeeded(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (ClassBuildManager.hasActiveCharacter(player)) {
            player.getServer().execute(() -> ClassBuildManager.applyLoadout(player));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ClassBuildManager.isFeatureEnabled()) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!ClassBuildManager.hasActiveCharacter(player) || player.isCreative()) {
                continue;
            }
            // Immediate sweep — never leave pickups sitting in vanilla slots.
            StowedInventory.scrubVanillaSlots(player);
        }
        reconcileTicker++;
        if (reconcileTicker < 100) {
            return;
        }
        reconcileTicker = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (ClassBuildManager.hasActiveCharacter(player)) {
                ClassBuildManager.reconcile(player);
                EpicFightSoft.ensureCombatMode(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ClassBuildManager.hasActiveCharacter(serverPlayer) || serverPlayer.isCreative()) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        if (IronSpellsSoft.isManaged(stack) || isSpellbookItem(stack)) {
            event.setCanPickup(TriState.FALSE);
        }
        // World pickups call Inventory.add → InventoryMixin deposits into stowed (not hotbar/hand).
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.hasActiveCharacter(player)) {
            return;
        }
        if (IronSpellsSoft.isManaged(event.getEntity().getItem())) {
            event.setCanceled(true);
            ClassBuildManager.reconcile(player);
        }
    }

    public static boolean isStorageSlot(int inventoryIndex) {
        return inventoryIndex >= 9 && inventoryIndex <= 35;
    }

    /** True for any bag/hotbar index that must not receive items (0–35). */
    public static boolean isVanillaBagOrHotbarSlot(int inventoryIndex) {
        return inventoryIndex >= 0 && inventoryIndex <= 35;
    }

    /**
     * Block placing into vanilla player slots; deposit the carried stack into stowed instead.
     */
    public static boolean denyStorageInsert(Player player, Slot slot, ItemStack carried) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (serverPlayer.isCreative()) {
            return false;
        }
        if (!ClassBuildManager.hasActiveCharacter(serverPlayer)) {
            return false;
        }
        if (slot == null || slot.container != player.getInventory()) {
            return false;
        }
        if (!isVanillaBagOrHotbarSlot(slot.getContainerSlot())) {
            return false;
        }
        if (carried.isEmpty()) {
            return false;
        }
        StowedInventory.stowPickup(serverPlayer, carried);
        return true;
    }

    private static boolean isSpellbookItem(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.startsWith("irons_spellbooks:") && id.contains("spell_book");
    }
}
