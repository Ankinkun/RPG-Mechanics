package com.ankin.rpgmechanics.quest;

import com.ankin.rpgmechanics.RpgMechanics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID)
public final class QuestEvents {
    private QuestEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestReloadListener());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(QuestCommands.register());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (QuestPermissions.isAuthoringEnabled()) {
            // Re-load export folder with full registry context so item/block predicates survive.
            QuestAuthoringIO.reloadOverlayFromDisk(event.getServer().registryAccess());
            QuestManager.purgeMissingDefinitionsForAll(event.getServer());
            QuestSync.syncDefinitionsToAll(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestManager.purgeMissingDefinitions(serverPlayer);
            QuestSync.syncAllToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestSync.syncPlayerState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer) {
            // copyOnDeath on the attachment handles persistence; re-sync after clone.
            QuestSync.syncPlayerState(newPlayer);
        }
    }
}
