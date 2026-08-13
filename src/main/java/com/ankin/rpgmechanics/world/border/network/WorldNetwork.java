package com.ankin.rpgmechanics.world.border.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WorldNetwork {
    private WorldNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WorldClientNetworkBootstrap.register(registrar);
        } else {
            registrar.playToClient(SyncWorldBorderPayload.TYPE, SyncWorldBorderPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(SyncBorderDraftPayload.TYPE, SyncBorderDraftPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(ToggleBorderDebugWallPayload.TYPE, ToggleBorderDebugWallPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToServer(
                    BorderWandActionPayload.TYPE,
                    BorderWandActionPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            BorderWandActionPayload.handle(payload, serverPlayer);
                        }
                    })
            );
        }
    }
}
