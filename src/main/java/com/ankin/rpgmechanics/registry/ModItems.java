package com.ankin.rpgmechanics.registry;

import java.util.function.Supplier;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.item.QuestEditorItem;
import com.ankin.rpgmechanics.world.border.item.BorderWandItem;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RpgMechanics.MOD_ID);

    public static final DeferredItem<Item> QUEST_EDITOR = ITEMS.register(
            "quest_editor",
            () -> new QuestEditorItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> BORDER_WAND = ITEMS.register(
            "border_wand",
            () -> new BorderWandItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
