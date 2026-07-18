package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestPermissions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenQuestEditorPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestOpenQuestEditorPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "request_open_quest_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenQuestEditorPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestOpenQuestEditorPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RequestOpenQuestEditorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!QuestPermissions.canUseEditor(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.rpgmechanics.quest.authoring_denied"));
                return;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new OpenQuestEditorPayload());
        });
    }
}
