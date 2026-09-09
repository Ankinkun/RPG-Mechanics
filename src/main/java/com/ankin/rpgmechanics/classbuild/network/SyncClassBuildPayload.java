package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncClassBuildPayload(ClassBuildState state) implements CustomPacketPayload {
    public static final Type<SyncClassBuildPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "sync_class_build"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncClassBuildPayload> STREAM_CODEC = StreamCodec.composite(
            ClassBuildState.STREAM_CODEC,
            SyncClassBuildPayload::state,
            SyncClassBuildPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
