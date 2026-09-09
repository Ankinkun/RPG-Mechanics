package com.ankin.rpgmechanics.classbuild;

import com.ankin.rpgmechanics.classbuild.integration.IronSpellsSoft;
import com.ankin.rpgmechanics.classbuild.network.OpenClassSelectPayload;
import com.ankin.rpgmechanics.classbuild.network.SyncClassBuildPayload;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.ankin.rpgmechanics.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClassBuildManager {
    private ClassBuildManager() {
    }

    public static boolean isFeatureEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildEnabled.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static int spellLevel() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return 3;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildSpellLevel.get();
        } catch (IllegalStateException exception) {
            return 3;
        }
    }

    public static boolean forceAdventure() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.classbuildForceAdventure.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static ClassBuildState get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_CLASS_BUILD);
    }

    public static void set(ServerPlayer player, ClassBuildState state) {
        player.setData(ModAttachments.PLAYER_CLASS_BUILD, state);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncClassBuildPayload(get(player)));
    }

    public static void openSelectIfNeeded(ServerPlayer player) {
        if (!isFeatureEnabled()) {
            return;
        }
        ClassBuildState state = get(player);
        if (!state.confirmed()) {
            PacketDistributor.sendToPlayer(player, new OpenClassSelectPayload());
        }
    }

    public static String confirm(ServerPlayer player, ClassBuildState draft) {
        if (!isFeatureEnabled()) {
            return "Class buildcrafting is disabled";
        }
        ClassBuildState next = new ClassBuildState(
                true,
                ClassBuildState.ClassRole.DAMAGE_DEALER,
                ClassBuildState.ElementTrack.DARKNESS,
                draft.melee(),
                draft.movement(),
                draft.ranged(),
                draft.ultimate()
        );
        var error = next.validationError();
        if (error.isPresent()) {
            return error.get();
        }
        set(player, next);
        applyLoadout(player);
        if (forceAdventure()) {
            player.setGameMode(GameType.ADVENTURE);
        }
        return null;
    }

    public static void applyLoadout(ServerPlayer player) {
        ClassBuildState state = get(player);
        if (!state.confirmed()) {
            return;
        }
        IronSpellsSoft.apply(player, state, spellLevel());
    }

    public static void reconcile(ServerPlayer player) {
        ClassBuildState state = get(player);
        if (!state.confirmed()) {
            return;
        }
        IronSpellsSoft.reconcile(player, state, spellLevel());
    }

    public static void reset(ServerPlayer player) {
        set(player, ClassBuildState.EMPTY);
        openSelectIfNeeded(player);
    }
}
