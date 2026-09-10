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

public record SelectCharacterPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<SelectCharacterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "select_character"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectCharacterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SelectCharacterPayload::slotIndex,
            SelectCharacterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectCharacterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String error = ClassBuildManager.selectSlot(player, payload.slotIndex());
            if (error != null) {
                player.sendSystemMessage(Component.literal(error));
            }
        });
    }
}
