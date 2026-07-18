package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.PlayerQuestState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncPlayerQuestStatePayload(PlayerQuestState state) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPlayerQuestStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "sync_player_quest_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerQuestStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(PlayerQuestState.CODEC),
            SyncPlayerQuestStatePayload::state,
            SyncPlayerQuestStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
