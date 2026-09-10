package com.ankin.rpgmechanics.classbuild.client;

import com.ankin.rpgmechanics.classbuild.ClassBuildState;
import com.ankin.rpgmechanics.classbuild.network.ConfirmClassBuildPayload;
import com.ankin.rpgmechanics.classbuild.network.SelectCharacterPayload;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Title-screen selection applied after the campaign world finishes joining.
 */
public final class TitleSelectPending {
    private static int pendingSlot = -1;
    private static ClassBuildState pendingConfirm;
    private static boolean fromTitleFlow;
    private static boolean suppressInWorldSelect;

    private TitleSelectPending() {
    }

    public static void clear() {
        pendingSlot = -1;
        pendingConfirm = null;
        fromTitleFlow = false;
    }

    public static boolean isFromTitleFlow() {
        return fromTitleFlow;
    }

    public static boolean suppressInWorldSelect() {
        return suppressInWorldSelect;
    }

    public static void clearSuppress() {
        suppressInWorldSelect = false;
    }

    public static void playExisting(int slot) {
        fromTitleFlow = true;
        suppressInWorldSelect = true;
        pendingSlot = slot;
        pendingConfirm = null;
        CampaignWorldLoader.loadCampaignOrNotify();
    }

    public static void playNew(int slot, ClassBuildState draft) {
        fromTitleFlow = true;
        suppressInWorldSelect = true;
        pendingSlot = slot;
        pendingConfirm = draft;
        ClientLocalRoster.createFromDraft(slot, draft);
        CampaignWorldLoader.loadCampaignOrNotify();
    }

    /** Called once the client has a player and network after login sync. */
    public static void flushIfPending() {
        if (!fromTitleFlow || pendingSlot < 0) {
            return;
        }
        int slot = pendingSlot;
        ClassBuildState draft = pendingConfirm;
        clear();
        if (draft != null) {
            PacketDistributor.sendToServer(new ConfirmClassBuildPayload(slot, draft.confirmedCopy()));
        } else {
            PacketDistributor.sendToServer(new SelectCharacterPayload(slot));
        }
    }
}
