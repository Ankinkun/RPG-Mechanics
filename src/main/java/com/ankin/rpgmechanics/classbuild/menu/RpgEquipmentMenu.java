package com.ankin.rpgmechanics.classbuild.menu;

import com.ankin.rpgmechanics.registry.ModMenus;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Slim equipment menu: helmet, chest, legs, boots, weapon (hotbar 0), offhand.
 */
public class RpgEquipmentMenu extends AbstractContainerMenu {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public RpgEquipmentMenu(int containerId, Inventory inventory) {
        super(ModMenus.RPG_EQUIPMENT.get(), containerId);

        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            final int armorIndex = 39 - i;
            this.addSlot(new Slot(inventory, armorIndex, 26, 8 + i * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(slot, inventory.player);
                }

                @Override
                public boolean mayPickup(Player player) {
                    ItemStack stack = this.getItem();
                    return (stack.isEmpty() || player.isCreative()
                            || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                            && super.mayPickup(player);
                }
            });
        }

        this.addSlot(new Slot(inventory, 0, 98, 26));
        this.addSlot(new Slot(inventory, 40, 98, 48));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
