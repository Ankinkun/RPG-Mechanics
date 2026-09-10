package com.ankin.rpgmechanics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.StowedInventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Redirects vanilla inventory inserts into the Destiny stowed bag so pickups never land in hotbar/hand.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void rpgmechanics$stowInsteadOfAdd(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!redirectToStow(stack)) {
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void rpgmechanics$stowInsteadOfAddIndexed(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!redirectToStow(stack)) {
            return;
        }
        cir.setReturnValue(true);
    }

    private boolean redirectToStow(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (StowedInventory.isApplyingEquipment()) {
            return false;
        }
        Inventory self = (Inventory) (Object) this;
        Player player = self.player;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (serverPlayer.isCreative()) {
            return false;
        }
        if (!ClassBuildManager.isFeatureEnabled() || !ClassBuildManager.hasActiveCharacter(serverPlayer)) {
            return false;
        }
        return StowedInventory.stowPickup(serverPlayer, stack);
    }
}
