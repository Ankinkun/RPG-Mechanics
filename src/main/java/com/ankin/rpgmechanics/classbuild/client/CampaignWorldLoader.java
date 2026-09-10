package com.ankin.rpgmechanics.classbuild.client;

import java.util.Optional;

import com.ankin.rpgmechanics.RpgMechanics;
import com.ankin.rpgmechanics.config.RpgMechanicsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * Loads the configured campaign singleplayer world (dev: shared by all characters).
 */
public final class CampaignWorldLoader {
    private CampaignWorldLoader() {
    }

    public static void loadCampaignOrNotify() {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<String> levelId = resolveLevelId(minecraft);
        if (levelId.isEmpty()) {
            RpgMechanics.LOGGER.warn("No campaign world found to load");
            minecraft.setScreen(new TitleScreen());
            return;
        }
        String id = levelId.get();
        RpgMechanics.LOGGER.info("Loading campaign world '{}'", id);
        minecraft.createWorldOpenFlows().openWorld(id, () -> minecraft.setScreen(new TitleScreen()));
    }

    private static Optional<String> resolveLevelId(Minecraft minecraft) {
        String configured = "";
        if (RpgMechanicsConfig.CLIENT_SPEC.isLoaded()) {
            try {
                configured = RpgMechanicsConfig.CLIENT.classbuildCampaignWorldName.get();
            } catch (IllegalStateException ignored) {
                configured = "";
            }
        }
        try {
            LevelStorageSource.LevelCandidates candidates = minecraft.getLevelSource().findLevelCandidates();
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            if (configured != null && !configured.isBlank()) {
                for (LevelStorageSource.LevelDirectory dir : candidates.levels()) {
                    if (configured.equals(dir.directoryName())) {
                        return Optional.of(dir.directoryName());
                    }
                }
                RpgMechanics.LOGGER.warn(
                        "Configured campaignWorldName '{}' not found; falling back to first save",
                        configured
                );
            }
            return Optional.of(candidates.levels().getFirst().directoryName());
        } catch (Exception exception) {
            RpgMechanics.LOGGER.error("Failed to list singleplayer worlds", exception);
            return Optional.empty();
        }
    }
}
