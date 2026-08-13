package com.ankin.rpgmechanics.registry;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpgMechanics.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rpgmechanics"))
                    .icon(() -> new ItemStack(ModItems.QUEST_EDITOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.QUEST_EDITOR.get());
                        output.accept(ModItems.BORDER_WAND.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}
