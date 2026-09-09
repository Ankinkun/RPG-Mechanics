package com.ankin.rpgmechanics.registry;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.menu.RpgEquipmentMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RpgMechanics.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<RpgEquipmentMenu>> RPG_EQUIPMENT = MENUS.register(
            "rpg_equipment",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new RpgEquipmentMenu(id, inv))
    );

    private ModMenus() {
    }
}
