package com.ankin.rpgmechanics.quest;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;

public record QuestStep(
        String id,
        String title,
        String description,
        Criterion<?> criterion
) {
    public static final Codec<QuestStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(QuestStep::id),
            Codec.STRING.fieldOf("title").forGetter(QuestStep::title),
            Codec.STRING.optionalFieldOf("description", "").forGetter(QuestStep::description),
            Criterion.CODEC.optionalFieldOf("criterion", manualCriterion()).forGetter(QuestStep::criterion)
    ).apply(instance, QuestStep::new));

    public static Criterion<?> manualCriterion() {
        return CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());
    }

    public QuestStep withCriterion(Criterion<?> newCriterion) {
        return new QuestStep(id, title, description, newCriterion);
    }
}
