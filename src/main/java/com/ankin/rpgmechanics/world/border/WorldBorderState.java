package com.ankin.rpgmechanics.world.border;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.ankin.rpgmechanics.world.border.network.SyncWorldBorderPayload;
import com.ankin.rpgmechanics.world.border.network.WorldBorderSync;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Runtime registry of active borders (custom JSON or vanilla-mimic default).
 */
public final class WorldBorderState {
    private static final Map<ResourceLocation, WorldBorderDefinition> borders = new HashMap<>();
    /** In-progress admin edit polygons (not yet saved). */
    private static final Map<ResourceLocation, BorderPolygon> drafts = new HashMap<>();

    private WorldBorderState() {
    }

    public static boolean isBorderFeatureEnabled() {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return true;
        }
        try {
            return RpgMechanicsConfig.SERVER.borderEnabled.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    public static Map<ResourceLocation, WorldBorderDefinition> all() {
        return Collections.unmodifiableMap(borders);
    }

    public static WorldBorderDefinition getOrDefault(ResourceLocation dimension) {
        WorldBorderDefinition existing = borders.get(dimension);
        if (existing != null) {
            return existing;
        }
        return WorldBorderDefinition.vanillaMimic(dimension);
    }

    public static void reload(MinecraftServer server) {
        borders.clear();
        borders.putAll(WorldBorderIO.loadAll());
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation id = level.dimension().location();
            borders.computeIfAbsent(id, WorldBorderDefinition::vanillaMimic);
            suppressVanillaBorder(level);
        }
        WorldBorderSync.syncAll(server);
        RpgMechanics.LOGGER.info("World borders ready: {} definition(s)", borders.size());
    }

    public static void ensureDimension(ServerLevel level) {
        ResourceLocation id = level.dimension().location();
        borders.computeIfAbsent(id, WorldBorderDefinition::vanillaMimic);
        if (isBorderFeatureEnabled()) {
            suppressVanillaBorder(level);
        }
    }

    public static void put(WorldBorderDefinition definition) {
        borders.put(definition.dimension(), definition);
    }

    public static void resetToVanillaMimic(ResourceLocation dimension) throws Exception {
        WorldBorderIO.delete(dimension);
        borders.put(dimension, WorldBorderDefinition.vanillaMimic(dimension));
        drafts.remove(dimension);
    }

    public static Optional<BorderPolygon> draft(ResourceLocation dimension) {
        return Optional.ofNullable(drafts.get(dimension));
    }

    public static BorderPolygon draftOrEmpty(ResourceLocation dimension) {
        return drafts.getOrDefault(dimension, new BorderPolygon(java.util.List.of()));
    }

    public static void setDraft(ResourceLocation dimension, BorderPolygon polygon) {
        drafts.put(dimension, polygon);
    }

    public static void clearDraft(ResourceLocation dimension) {
        drafts.remove(dimension);
    }

    /** All in-progress drafts (for login sync of non-current dims if needed). */
    public static Map<ResourceLocation, BorderPolygon> allDrafts() {
        return Collections.unmodifiableMap(drafts);
    }

    /**
     * Expand vanilla border to max and zero its damage so our fog system owns the feel.
     */
    public static void suppressVanillaBorder(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(WorldBorder.MAX_SIZE);
        border.setDamagePerBlock(0.0);
        border.setDamageSafeZone(WorldBorder.MAX_SIZE);
        border.setWarningBlocks(0);
        border.setWarningTime(0);
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (!isBorderFeatureEnabled()) {
            PacketDistributor.sendToPlayer(player, SyncWorldBorderPayload.featureOff());
            return;
        }
        PacketDistributor.sendToPlayer(player, SyncWorldBorderPayload.beginSync());
        for (WorldBorderDefinition definition : borders.values()) {
            PacketDistributor.sendToPlayer(player, SyncWorldBorderPayload.from(definition));
        }
        WorldBorderSync.syncDraftToPlayer(player);
    }

    @Nullable
    public static WorldBorderDefinition forLevel(Level level) {
        if (!isBorderFeatureEnabled()) {
            return null;
        }
        return getOrDefault(level.dimension().location());
    }
}
