package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeleteCharacterPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<DeleteCharacterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "delete_character"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteCharacterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DeleteCharacterPayload::slotIndex,
            DeleteCharacterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteCharacterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String error = ClassBuildManager.deleteSlot(player, payload.slotIndex());
            if (error != null) {
                player.sendSystemMessage(Component.literal(error));
            }
        });
    }
}
