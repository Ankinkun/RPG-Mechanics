package com.ankin.rpgmechanics.quest.network;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenQuestEditorPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenQuestEditorPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "open_quest_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestEditorPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenQuestEditorPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
