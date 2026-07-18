package com.ankin.rpgmechanics.quest.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Curated quest detection methods offered in the editor (not the full trigger registry).
 */
public final class DetectionMethods {
    public record Entry(ResourceLocation triggerId, Component label) {
    }

    public static final List<Entry> ALL = List.of(
            new Entry(ResourceLocation.withDefaultNamespace("location"), Component.translatable("screen.rpgmechanics.detection_biome")),
            new Entry(ResourceLocation.withDefaultNamespace("inventory_changed"), Component.translatable("screen.rpgmechanics.detection_item")),
            new Entry(ResourceLocation.withDefaultNamespace("placed_block"), Component.translatable("screen.rpgmechanics.detection_place_block")),
            new Entry(ResourceLocation.withDefaultNamespace("player_killed_entity"), Component.translatable("screen.rpgmechanics.detection_kill"))
    );

    private DetectionMethods() {
    }

    public static List<ResourceLocation> triggerIds() {
        return ALL.stream().map(Entry::triggerId).toList();
    }

    public static Component labelFor(ResourceLocation triggerId) {
        if (triggerId == null) {
            return Component.literal("?");
        }
        for (Entry entry : ALL) {
            if (entry.triggerId().equals(triggerId)) {
                return entry.label();
            }
        }
        return Component.literal(triggerId.getPath());
    }
}
