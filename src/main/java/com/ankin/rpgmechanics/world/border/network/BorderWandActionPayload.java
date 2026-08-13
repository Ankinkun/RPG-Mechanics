package com.ankin.rpgmechanics.world.border.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.WorldBorderAuthoring;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Border wand → server. Client sends desired action; server applies draft edit.
 */
public record BorderWandActionPayload(Action action) implements CustomPacketPayload {
    public static final Type<BorderWandActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "border_wand_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BorderWandActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeEnum(payload.action),
                    buf -> new BorderWandActionPayload(buf.readEnum(Action.class))
            );

    public enum Action {
        ADD,
        UNDO,
        CLEAR
    }

    public static void handle(BorderWandActionPayload payload, ServerPlayer player) {
        if (!WorldBorderAuthoring.canAuthor(player)) {
            player.sendSystemMessage(Component.literal("Border wand requires permission level 2+"));
            return;
        }
        Component message = switch (payload.action) {
            case ADD -> {
                double x = player.getX();
                double z = player.getZ();
                HitResult hit = player.pick(20.0, 0.0F, false);
                if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                    x = blockHit.getBlockPos().getX() + 0.5;
                    z = blockHit.getBlockPos().getZ() + 0.5;
                }
                yield WorldBorderAuthoring.addVertex(player, x, z);
            }
            case UNDO -> WorldBorderAuthoring.undo(player);
            case CLEAR -> WorldBorderAuthoring.clear(player);
        };
        player.sendSystemMessage(message);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
