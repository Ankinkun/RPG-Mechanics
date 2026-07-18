package com.ankin.rpgmechanics.quest.item;

import com.ankin.rpgmechanics.quest.network.RequestOpenQuestEditorPayload;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class QuestEditorItem extends Item {
    public QuestEditorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            PacketDistributor.sendToServer(new RequestOpenQuestEditorPayload());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
