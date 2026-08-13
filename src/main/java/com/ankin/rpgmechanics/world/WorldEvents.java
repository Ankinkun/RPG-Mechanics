package com.ankin.rpgmechanics.world;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.world.border.WorldBorderIO;
import com.ankin.rpgmechanics.world.border.WorldBorderState;
import com.ankin.rpgmechanics.world.protection.WorldProtection;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID)
public final class WorldEvents {
    private WorldEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(WorldCommands.register());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            WorldBorderIO.ensureDirectory(event.getServer());
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to create world border directory", exception);
        }
        WorldBorderState.reload(event.getServer());
        RpgMechanics.LOGGER.info(
                "World module: protection={} opsBypass={} border={}",
                WorldProtection.isEnabled(),
                WorldProtection.opsBypass(),
                WorldBorderState.isBorderFeatureEnabled()
        );
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && WorldBorderState.isBorderFeatureEnabled()) {
            WorldBorderState.ensureDimension(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (WorldBorderState.isBorderFeatureEnabled() && serverPlayer.level() instanceof ServerLevel serverLevel) {
                WorldBorderState.suppressVanillaBorder(serverLevel);
            }
            WorldBorderState.syncToPlayer(serverPlayer);
            RpgMechanics.LOGGER.debug(
                    "World sync {}: canBuild={} protection={}",
                    serverPlayer.getGameProfile().getName(),
                    WorldProtection.isBuilder(serverPlayer),
                    WorldProtection.isEnabled()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (WorldBorderState.isBorderFeatureEnabled() && serverPlayer.level() instanceof ServerLevel serverLevel) {
                WorldBorderState.suppressVanillaBorder(serverLevel);
            }
            WorldBorderState.syncToPlayer(serverPlayer);
        }
    }
}
