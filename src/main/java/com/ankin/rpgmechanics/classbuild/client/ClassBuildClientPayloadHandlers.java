package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.CharacterRoster;
import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.network.OpenClassSelectPayload;
import com.ankin.rpgmechanics.classbuild.network.SyncClassBuildPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClassBuildClientPayloadHandlers {
    private static CharacterRoster cached = CharacterRoster.EMPTY;
    private static boolean pendingSelect;

    private ClassBuildClientPayloadHandlers() {
    }

    public static CharacterRoster cachedRoster() {
        return cached;
    }

    public static ClassBuildState cached() {
        return cached.activeBuildOrEmpty();
    }

    public static boolean hasActiveCharacter() {
        return cached.hasActive();
    }

    public static boolean pendingSelect() {
        return pendingSelect;
    }

    public static void clearPendingSelect() {
        pendingSelect = false;
    }

    public static void handleSync(SyncClassBuildPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            cached = payload.roster();
            Minecraft minecraft = Minecraft.getInstance();
            if ((TitleSelectPending.isFromTitleFlow() || TitleSelectPending.suppressInWorldSelect())
                    && minecraft.player != null
                    && !cached.hasActive()) {
                TitleSelectPending.flushIfPending();
                pendingSelect = false;
                return;
            }
            if (cached.hasActive()) {
                pendingSelect = false;
                TitleSelectPending.clearSuppress();
                if (minecraft.screen instanceof CharacterSelectScreen
                        || minecraft.screen instanceof ClassSelectScreen) {
                    minecraft.setScreen(null);
                }
                // Keep local roster in sync for next title session
                ClientLocalRoster.save(cached.clearActive());
            } else if (minecraft.screen instanceof CharacterSelectScreen selectScreen) {
                selectScreen.refresh();
            }
        });
    }

    public static void handleOpenSelect(OpenClassSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (TitleSelectPending.isFromTitleFlow() || TitleSelectPending.suppressInWorldSelect()) {
                if (minecraft.player != null) {
                    TitleSelectPending.flushIfPending();
                }
                pendingSelect = false;
                return;
            }
            pendingSelect = true;
            if (minecraft.player != null) {
                minecraft.setScreen(new CharacterSelectScreen(false));
                pendingSelect = false;
            }
        });
    }
}
