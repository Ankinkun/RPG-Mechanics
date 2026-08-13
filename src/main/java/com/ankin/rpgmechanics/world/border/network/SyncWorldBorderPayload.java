package com.ankin.rpgmechanics.world.border.network;

import java.util.ArrayList;
import java.util.List;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client sync for fog borders.
 * <ul>
 *   <li>{@code clearAll && !featureEnabled} — feature off, wipe cache</li>
 *   <li>{@code clearAll && featureEnabled} — wipe cache, feature stays on (resync start)</li>
 *   <li>{@code !clearAll} — upsert one dimension definition</li>
 * </ul>
 */
public record SyncWorldBorderPayload(
        boolean clearAll,
        boolean featureEnabled,
        ResourceLocation dimension,
        boolean enabled,
        boolean custom,
        List<BorderPolygon.Vertex> vertices,
        double fogDepth,
        double softMargin,
        double maxDamagePerSecond
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncWorldBorderPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RpgMechanics.MOD_ID, "sync_world_border"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWorldBorderPayload> STREAM_CODEC =
            StreamCodec.of(SyncWorldBorderPayload::encode, SyncWorldBorderPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncWorldBorderPayload payload) {
        buf.writeBoolean(payload.clearAll);
        buf.writeBoolean(payload.featureEnabled);
        buf.writeResourceLocation(payload.dimension);
        buf.writeBoolean(payload.enabled);
        buf.writeBoolean(payload.custom);
        buf.writeVarInt(payload.vertices.size());
        for (BorderPolygon.Vertex vertex : payload.vertices) {
            buf.writeDouble(vertex.x());
            buf.writeDouble(vertex.z());
        }
        buf.writeDouble(payload.fogDepth);
        buf.writeDouble(payload.softMargin);
        buf.writeDouble(payload.maxDamagePerSecond);
    }

    private static SyncWorldBorderPayload decode(RegistryFriendlyByteBuf buf) {
        boolean clearAll = buf.readBoolean();
        boolean featureEnabled = buf.readBoolean();
        ResourceLocation dimension = buf.readResourceLocation();
        boolean enabled = buf.readBoolean();
        boolean custom = buf.readBoolean();
        int count = buf.readVarInt();
        List<BorderPolygon.Vertex> vertices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            vertices.add(new BorderPolygon.Vertex(buf.readDouble(), buf.readDouble()));
        }
        return new SyncWorldBorderPayload(
                clearAll,
                featureEnabled,
                dimension,
                enabled,
                custom,
                List.copyOf(vertices),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static SyncWorldBorderPayload featureOff() {
        return new SyncWorldBorderPayload(
                true,
                false,
                ResourceLocation.withDefaultNamespace("overworld"),
                false,
                false,
                List.of(),
                0.0,
                0.0,
                0.0
        );
    }

    /** Clears client cache but keeps the fog-border feature active for the following upserts. */
    public static SyncWorldBorderPayload beginSync() {
        return new SyncWorldBorderPayload(
                true,
                true,
                ResourceLocation.withDefaultNamespace("overworld"),
                false,
                false,
                List.of(),
                0.0,
                0.0,
                0.0
        );
    }

    public static SyncWorldBorderPayload from(WorldBorderDefinition definition) {
        return new SyncWorldBorderPayload(
                false,
                true,
                definition.dimension(),
                definition.enabled(),
                definition.custom(),
                definition.polygon().vertices(),
                definition.fogDepth(),
                definition.softMargin(),
                definition.maxDamagePerSecond()
        );
    }

    public WorldBorderDefinition toDefinition() {
        return new WorldBorderDefinition(
                dimension,
                enabled,
                custom,
                new BorderPolygon(vertices),
                fogDepth,
                softMargin,
                maxDamagePerSecond
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
