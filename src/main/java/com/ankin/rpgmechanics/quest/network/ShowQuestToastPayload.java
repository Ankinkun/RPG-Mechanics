package com.ankin.rpgmechanics.quest.network;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.quest.QuestProgressEvent;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShowQuestToastPayload(
        ResourceLocation questId,
        QuestProgressEvent event,
        String questTitle,
        String stepTitle,
        @Nullable ResourceLocation icon
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShowQuestToastPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "show_quest_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShowQuestToastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, payload.questId());
                buf.writeUtf(payload.event().name());
                buf.writeUtf(payload.questTitle());
                buf.writeUtf(payload.stepTitle());
                buf.writeBoolean(payload.icon() != null);
                if (payload.icon() != null) {
                    ResourceLocation.STREAM_CODEC.encode(buf, payload.icon());
                }
            },
            buf -> {
                ResourceLocation questId = ResourceLocation.STREAM_CODEC.decode(buf);
                QuestProgressEvent event = QuestProgressEvent.valueOf(buf.readUtf());
                String questTitle = buf.readUtf();
                String stepTitle = buf.readUtf();
                ResourceLocation icon = buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null;
                return new ShowQuestToastPayload(questId, event, questTitle, stepTitle, icon);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
