package com.ankin.rpgmechanics.world.border.network;

import java.util.ArrayList;
import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.BorderPolygon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Syncs the in-progress border draft polygon to clients (for admin preview). */
public record SyncBorderDraftPayload(
        ResourceLocation dimension,
        List<BorderPolygon.Vertex> vertices
) implements CustomPacketPayload {
    public static final Type<SyncBorderDraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "sync_border_draft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBorderDraftPayload> STREAM_CODEC =
            StreamCodec.of(SyncBorderDraftPayload::encode, SyncBorderDraftPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncBorderDraftPayload payload) {
        buf.writeResourceLocation(payload.dimension);
        buf.writeVarInt(payload.vertices.size());
        for (BorderPolygon.Vertex vertex : payload.vertices) {
            buf.writeDouble(vertex.x());
            buf.writeDouble(vertex.z());
        }
    }

    private static SyncBorderDraftPayload decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<BorderPolygon.Vertex> vertices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            vertices.add(new BorderPolygon.Vertex(buf.readDouble(), buf.readDouble()));
        }
        return new SyncBorderDraftPayload(dimension, List.copyOf(vertices));
    }

    public static SyncBorderDraftPayload clear(ResourceLocation dimension) {
        return new SyncBorderDraftPayload(dimension, List.of());
    }

    public static SyncBorderDraftPayload of(ResourceLocation dimension, BorderPolygon polygon) {
        return new SyncBorderDraftPayload(dimension, polygon.vertices());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
