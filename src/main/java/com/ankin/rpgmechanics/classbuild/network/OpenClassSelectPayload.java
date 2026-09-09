package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenClassSelectPayload() implements CustomPacketPayload {
    public static final Type<OpenClassSelectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "open_class_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClassSelectPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenClassSelectPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
