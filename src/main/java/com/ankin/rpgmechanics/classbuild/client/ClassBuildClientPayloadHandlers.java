package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.network.OpenClassSelectPayload;
import com.ankin.rpgmechanics.classbuild.network.SyncClassBuildPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClassBuildClientPayloadHandlers {
    private static ClassBuildState cached = ClassBuildState.EMPTY;
    private static boolean pendingSelect;

    private ClassBuildClientPayloadHandlers() {
    }

    public static ClassBuildState cached() {
        return cached;
    }

    public static boolean pendingSelect() {
        return pendingSelect;
    }

    public static void clearPendingSelect() {
        pendingSelect = false;
    }

    public static void handleSync(SyncClassBuildPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            cached = payload.state();
            if (cached.confirmed()) {
                pendingSelect = false;
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen instanceof ClassSelectScreen) {
                    minecraft.setScreen(null);
                }
            }
        });
    }

    public static void handleOpenSelect(OpenClassSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            pendingSelect = true;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.setScreen(new ClassSelectScreen());
                pendingSelect = false;
            }
        });
    }
}
