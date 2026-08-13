package com.ankin.rpgmechanics.world.border.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.ankin.rpgmechanics.world.border.BorderPolygon;
import com.ankin.rpgmechanics.world.border.WorldBorderDefinition;
import com.ankin.rpgmechanics.world.border.network.SyncBorderDraftPayload;
import com.ankin.rpgmechanics.world.border.network.SyncWorldBorderPayload;

import net.minecraft.resources.ResourceLocation;

public final class ClientWorldBorderCache {
    private static final Map<ResourceLocation, WorldBorderDefinition> borders = new HashMap<>();
    private static final Map<ResourceLocation, BorderPolygon> drafts = new HashMap<>();
    private static boolean featureEnabled = true;

    private ClientWorldBorderCache() {
    }

    public static void apply(SyncWorldBorderPayload payload) {
        if (payload.clearAll()) {
            borders.clear();
            featureEnabled = payload.featureEnabled();
            return;
        }
        featureEnabled = true;
        borders.put(payload.dimension(), payload.toDefinition());
    }

    public static void applyDraft(SyncBorderDraftPayload payload) {
        if (payload.vertices().isEmpty()) {
            drafts.remove(payload.dimension());
        } else {
            drafts.put(payload.dimension(), new BorderPolygon(payload.vertices()));
        }
    }

    @Nullable
    public static BorderPolygon draft(ResourceLocation dimension) {
        return drafts.get(dimension);
    }

    public static boolean isFeatureEnabled() {
        return featureEnabled;
    }

    @Nullable
    public static WorldBorderDefinition get(ResourceLocation dimension) {
        WorldBorderDefinition existing = borders.get(dimension);
        if (existing != null) {
            return existing;
        }
        // Until the first sync arrives, mimic server zero-config default.
        if (featureEnabled) {
            return WorldBorderDefinition.vanillaMimic(dimension);
        }
        return null;
    }

    public static Map<ResourceLocation, WorldBorderDefinition> all() {
        return Map.copyOf(borders);
    }
}
