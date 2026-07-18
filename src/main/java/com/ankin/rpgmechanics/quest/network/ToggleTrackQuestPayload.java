package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleTrackQuestPayload(ResourceLocation questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleTrackQuestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "toggle_track_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleTrackQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ToggleTrackQuestPayload::questId,
            ToggleTrackQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleTrackQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                QuestManager.toggleTrack(serverPlayer, payload.questId());
            }
        });
    }
}
