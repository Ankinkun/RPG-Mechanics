package com.ankin.rpgmechanics.classbuild.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;

/**
 * Hard Epic Fight server bridge (compileOnly). Loaded only via {@link EpicFightSoft}.
 */
public final class EpicFightBridge {
    private EpicFightBridge() {
    }

    public static void grantStarterSkills(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) {
            return;
        }
        PlayerSkills skills = patch.getPlayerSkills();
        Skill roll = EpicFightSkills.ROLL.get();
        Skill guard = EpicFightSkills.GUARD.get();
        if (roll != null) {
            skills.addLearnedSkill(roll);
            SkillContainer dodge = skills.getSkillContainerFor(SkillSlots.DODGE);
            if (dodge != null) {
                dodge.setSkill(roll);
            }
        }
        if (guard != null) {
            skills.addLearnedSkill(guard);
            SkillContainer guardSlot = skills.getSkillContainerFor(SkillSlots.GUARD);
            if (guardSlot != null) {
                guardSlot.setSkill(guard);
            }
        }
    }

    public static boolean isTwoHanded(ServerPlayer player, ItemStack stack) {
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(stack);
        if (capability == null) {
            return false;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) {
            return false;
        }
        Style style = capability.getStyle(patch);
        return style != null && !style.canUseOffhand();
    }

    public static void ensureCombatMode(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) {
            return;
        }
        if (!patch.isEpicFightMode()) {
            patch.toEpicFightMode(true);
        }
    }
}
