package com.ankin.rpgmechanics.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ankin.rpgmechanics.quest.QuestCriterionBridge;

import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

@Mixin(SimpleCriterionTrigger.class)
public abstract class SimpleCriterionTriggerMixin {
    @Inject(
            method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Predicate;)V",
            at = @At("HEAD")
    )
    private void rpgmechanics$onTrigger(ServerPlayer player, Predicate<?> test, CallbackInfo ci) {
        QuestCriterionBridge.onCriterionTrigger((SimpleCriterionTrigger<?>) (Object) this, player, test);
    }
}
