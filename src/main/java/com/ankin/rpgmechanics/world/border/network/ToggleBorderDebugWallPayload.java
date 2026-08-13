package com.ankin.rpgmechanics.world.border.network;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client toggle for the light-blue debug border wall. */
public record ToggleBorderDebugWallPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleBorderDebugWallPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "toggle_border_debug_wall"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleBorderDebugWallPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleBorderDebugWallPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
