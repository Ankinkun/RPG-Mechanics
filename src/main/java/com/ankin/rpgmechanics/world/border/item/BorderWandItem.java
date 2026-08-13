package com.ankin.rpgmechanics.world.border.item;

import java.util.List;

import com.ankin.rpgmechanics.world.border.network.BorderWandActionPayload;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Admin tool: right-click places a draft border vertex (block look-at, else feet).
 * Sneak+right-click undoes the last vertex.
 */
public class BorderWandItem extends Item {
    public BorderWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            BorderWandActionPayload.Action action = player.isShiftKeyDown()
                    ? BorderWandActionPayload.Action.UNDO
                    : BorderWandActionPayload.Action.ADD;
            PacketDistributor.sendToServer(new BorderWandActionPayload(action));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rpgmechanics.border_wand.desc"));
    }
}
