package com.ankin.rpgmechanics.quest;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record QuestDefinition(
        ResourceLocation id,
        String title,
        String description,
        Optional<ResourceLocation> icon,
        boolean repeatable,
        List<QuestStep> steps
) {
    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(QuestDefinition::id),
            Codec.STRING.fieldOf("title").forGetter(QuestDefinition::title),
            Codec.STRING.optionalFieldOf("description", "").forGetter(QuestDefinition::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(QuestDefinition::icon),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(QuestDefinition::repeatable),
            QuestStep.CODEC.listOf().fieldOf("steps").forGetter(QuestDefinition::steps)
    ).apply(instance, QuestDefinition::new));

    public QuestDefinition(ResourceLocation id, String title, String description, List<QuestStep> steps) {
        this(id, title, description, Optional.empty(), false, steps);
    }

    public boolean hasSteps() {
        return steps != null && !steps.isEmpty();
    }

    public QuestStep stepAt(int index) {
        if (!hasSteps() || index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public boolean isComplete(int stepIndex) {
        return hasSteps() && stepIndex >= steps.size();
    }
}
