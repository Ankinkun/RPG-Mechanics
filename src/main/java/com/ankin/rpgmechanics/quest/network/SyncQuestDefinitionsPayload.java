package com.ankin.rpgmechanics.quest.network;

import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestDefinition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncQuestDefinitionsPayload(List<QuestDefinition> quests) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncQuestDefinitionsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "sync_quest_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuestDefinitionsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(QuestDefinition.CODEC.listOf()),
            SyncQuestDefinitionsPayload::quests,
            SyncQuestDefinitionsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
