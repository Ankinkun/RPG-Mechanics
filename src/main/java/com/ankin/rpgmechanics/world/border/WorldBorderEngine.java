package com.ankin.rpgmechanics.world.border;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RpgMechanics.MOD_ID)
public final class WorldBorderEngine {
    private static int tickCounter;

    private WorldBorderEngine() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!WorldBorderState.isBorderFeatureEnabled()) {
            return;
        }
        // Every 10 ticks (~0.5s) apply scaled damage so we are not hot every tick.
        if (++tickCounter % 10 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        double hardKill = hardKillDistance();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            WorldBorderDefinition definition = WorldBorderState.forLevel(player.level());
            if (definition == null || !definition.enabled() || !definition.polygon().isValid()) {
                continue;
            }
            double outside = definition.polygon().distanceOutside(player.getX(), player.getZ());
            if (outside <= definition.softMargin()) {
                continue;
            }
            if (hardKill > 0.0 && outside >= hardKill) {
                player.hurt(player.damageSources().source(DamageTypes.OUTSIDE_BORDER), 1000.0F);
                continue;
            }
            double depth = Math.max(0.0, outside - definition.softMargin());
            double ramp = Math.min(1.0, depth / Math.max(1.0, definition.fogDepth()));
            // 10-tick interval → damage for half a second of the per-second rate.
            float damage = (float) (definition.maxDamagePerSecond() * ramp * 0.5);
            if (damage > 0.0F) {
                player.hurt(player.damageSources().source(DamageTypes.OUTSIDE_BORDER), damage);
            }
        }
    }

    private static double hardKillDistance() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return 0.0;
        }
        try {
            return RpgMechanicsConfig.SERVER.borderHardKillDistance.get();
        } catch (IllegalStateException exception) {
            return 0.0;
        }
    }
}
