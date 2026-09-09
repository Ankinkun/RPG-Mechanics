package com.ankin.rpgmechanics.classbuild;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
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
        ClassBuildManager.sync(player);
        ClassBuildState state = ClassBuildManager.get(player);
        if (state.confirmed()) {
            ClassBuildManager.applyLoadout(player);
        } else {
            ClassBuildManager.openSelectIfNeeded(player);
        }
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
        if (ClassBuildManager.get(player).confirmed()) {
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
        if (ClassBuildManager.get(player).confirmed()) {
            player.getServer().execute(() -> ClassBuildManager.applyLoadout(player));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ClassBuildManager.isFeatureEnabled()) {
            return;
        }
        reconcileTicker++;
        if (reconcileTicker < 100) {
            return;
        }
        reconcileTicker = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (ClassBuildManager.get(player).confirmed()) {
                ClassBuildManager.reconcile(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ClassBuildManager.get(serverPlayer).confirmed()) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        if (IronSpellsSoft.isManaged(stack) || isSpellbookItem(stack)) {
            event.setCanPickup(TriState.FALSE);
            return;
        }
        if (!canFitInHotbarOrEquipment(serverPlayer, stack)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.get(player).confirmed()) {
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

    public static boolean denyStorageInsert(Player player, Slot slot, ItemStack carried) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!ClassBuildManager.get(serverPlayer).confirmed()) {
            return false;
        }
        if (slot == null || slot.container != player.getInventory()) {
            return false;
        }
        return isStorageSlot(slot.getContainerSlot()) && !carried.isEmpty();
    }

    private static boolean canFitInHotbarOrEquipment(ServerPlayer player, ItemStack stack) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack existing = inv.getItem(i);
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        ItemStack off = inv.offhand.getFirst();
        return off.isEmpty() || (ItemStack.isSameItemSameComponents(off, stack)
                && off.getCount() < off.getMaxStackSize());
    }

    private static boolean isSpellbookItem(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.startsWith("irons_spellbooks:") && id.contains("spell_book");
    }
}
