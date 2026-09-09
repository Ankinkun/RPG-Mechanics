package com.ankin.rpgmechanics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ankin.rpgmechanics.classbuild.ClassBuildEvents;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Blocks placing items into player inventory storage slots 9–35 when classbuild is active.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void rpgmechanics$denyStorageSlots(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= self.slots.size()) {
            return;
        }
        Slot slot = self.slots.get(slotId);
        ItemStack carried = self.getCarried();
        if (ClassBuildEvents.denyStorageInsert(player, slot, carried)) {
            ci.cancel();
            return;
        }
        // Shift-click into storage from other containers: block quick-move targeting storage.
        if (clickType == ClickType.QUICK_MOVE && slot != null && slot.container != player.getInventory()) {
            // Destination is resolved inside quickMoveStack — handled by returning EMPTY from equipment menu;
            // for chests, cancel quick-move entirely when classbuild confirmed and no hotbar space would be needed.
            // Safer: cancel quick-move into player inventory storage by checking after — skip here.
        }
    }
}
