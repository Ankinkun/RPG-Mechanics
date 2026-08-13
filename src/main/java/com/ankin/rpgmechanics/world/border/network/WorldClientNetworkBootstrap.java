package com.ankin.rpgmechanics.world.border.network;

import com.ankin.rpgmechanics.world.border.client.BorderDebugWallRenderer;
import com.ankin.rpgmechanics.world.border.client.ClientWorldBorderCache;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WorldClientNetworkBootstrap {
    private WorldClientNetworkBootstrap() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                SyncWorldBorderPayload.TYPE,
                SyncWorldBorderPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientWorldBorderCache.apply(payload))
        );
        registrar.playToClient(
                SyncBorderDraftPayload.TYPE,
                SyncBorderDraftPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientWorldBorderCache.applyDraft(payload))
        );
        registrar.playToClient(
                ToggleBorderDebugWallPayload.TYPE,
                ToggleBorderDebugWallPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    boolean on = BorderDebugWallRenderer.toggle();
                    if (context.player() != null) {
                        context.player().displayClientMessage(
                                Component.literal("Border debug wall: " + (on ? "ON" : "OFF")),
                                true
                        );
                    }
                })
        );
        registrar.playToServer(
                BorderWandActionPayload.TYPE,
                BorderWandActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        BorderWandActionPayload.handle(payload, serverPlayer);
                    }
                })
        );
    }
}
