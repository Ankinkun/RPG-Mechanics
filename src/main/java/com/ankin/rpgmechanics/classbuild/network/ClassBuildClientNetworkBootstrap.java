package com.ankin.rpgmechanics.classbuild.network;

import com.ankin.rpgmechanics.classbuild.client.ClassBuildClientPayloadHandlers;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClassBuildClientNetworkBootstrap {
    private ClassBuildClientNetworkBootstrap() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                SyncClassBuildPayload.TYPE,
                SyncClassBuildPayload.STREAM_CODEC,
                ClassBuildClientPayloadHandlers::handleSync
        );
        registrar.playToClient(
                OpenClassSelectPayload.TYPE,
                OpenClassSelectPayload.STREAM_CODEC,
                ClassBuildClientPayloadHandlers::handleOpenSelect
        );
    }
}
