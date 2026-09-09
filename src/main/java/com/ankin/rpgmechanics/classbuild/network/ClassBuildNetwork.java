package com.ankin.rpgmechanics.classbuild.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClassBuildNetwork {
    private ClassBuildNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ConfirmClassBuildPayload.TYPE,
                ConfirmClassBuildPayload.STREAM_CODEC,
                ConfirmClassBuildPayload::handle
        );
        registrar.playToServer(
                OpenRpgEquipmentPayload.TYPE,
                OpenRpgEquipmentPayload.STREAM_CODEC,
                OpenRpgEquipmentPayload::handle
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClassBuildClientNetworkBootstrap.register(registrar);
        } else {
            registrar.playToClient(SyncClassBuildPayload.TYPE, SyncClassBuildPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(OpenClassSelectPayload.TYPE, OpenClassSelectPayload.STREAM_CODEC, (payload, context) -> {
            });
        }
    }
}
