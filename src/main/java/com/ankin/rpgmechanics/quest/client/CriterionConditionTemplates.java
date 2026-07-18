package com.ankin.rpgmechanics.quest.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Example conditions JSON for the curated quest detection methods.
 */
public final class CriterionConditionTemplates {
    private CriterionConditionTemplates() {
    }

    public static String exampleFor(ResourceLocation triggerId) {
        if (triggerId == null) {
            return "{}";
        }
        return switch (triggerId.getPath()) {
            case "location" -> "{\"player\":[{\"location\":{\"biomes\":\"minecraft:plains\"}}]}";
            case "inventory_changed" -> "{\"items\":[{\"items\":\"minecraft:dirt\"}]}";
            case "placed_block" -> "{\"location\":[{\"block\":{\"blocks\":\"minecraft:crafting_table\"}}]}";
            case "player_killed_entity" -> "{\"entity\":[{\"type\":\"minecraft:zombie\"}]}";
            default -> "{}";
        };
    }
}
