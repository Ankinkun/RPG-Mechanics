package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildManager;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfirmClassBuildPayload(ClassBuildState draft) implements CustomPacketPayload {
    public static final Type<ConfirmClassBuildPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "confirm_class_build"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmClassBuildPayload> STREAM_CODEC = StreamCodec.composite(
            ClassBuildState.STREAM_CODEC,
            ConfirmClassBuildPayload::draft,
            ConfirmClassBuildPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfirmClassBuildPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String error = ClassBuildManager.confirm(player, payload.draft());
            if (error != null) {
                player.sendSystemMessage(Component.literal(error));
            }
        });
    }
}
