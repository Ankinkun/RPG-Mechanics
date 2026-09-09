package com.ankin.rpgmechanics.classbuild;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsBridge;
import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;

import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.event.CurioCanUnequipEvent;

/**
 * Registered only when Iron Spells + Curios are present ({@link IronSpellsSoft#registerIntegrationEvents()}).
 */
public final class ClassBuildIntegrationEvents {
    private ClassBuildIntegrationEvents() {
    }

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClassBuildIntegrationEvents.class);
        RpgMechanics.LOGGER.info("Class build Iron Spells/Curios integration enabled");
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.get(player).confirmed()) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof InscriptionTableMenu
                || menu instanceof ArcaneAnvilMenu
                || menu instanceof ScrollForgeMenu) {
            player.closeContainer();
            player.sendSystemMessage(Component.translatable("message.rpgmechanics.classbuild.spellbook_locked"));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.get(player).confirmed()) {
            return;
        }
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        if (id.equals("irons_spellbooks:inscription_table")
                || id.equals("irons_spellbooks:scroll_forge")
                || id.equals("irons_spellbooks:arcane_anvil")) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("message.rpgmechanics.classbuild.spellbook_locked"));
        }
    }

    @SubscribeEvent
    public static void onCurioUnequip(CurioCanUnequipEvent event) {
        if (Boolean.TRUE.equals(IronSpellsBridge.APPLYING.get())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.get(player).confirmed()) {
            return;
        }
        if ("spellbook".equals(event.getSlotContext().identifier())
                || IronSpellsSoft.isManaged(event.getStack())) {
            event.setUnequipResult(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onCurioEquip(CurioCanEquipEvent event) {
        if (Boolean.TRUE.equals(IronSpellsBridge.APPLYING.get())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ClassBuildManager.get(player).confirmed()) {
            return;
        }
        if (!"spellbook".equals(event.getSlotContext().identifier())) {
            return;
        }
        if (!IronSpellsSoft.isManaged(event.getStack())) {
            event.setEquipResult(TriState.FALSE);
        }
    }
}
