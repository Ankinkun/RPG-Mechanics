package com.ankin.rpgmechanics.classbuild;

import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

/** The six Destiny doll slots (armor + weapon + offhand). */
public enum GearSlot {
    HELMET,
    CHEST,
    LEGS,
    BOOTS,
    WEAPON,
    OFFHAND;

    public EquipmentSlot equipmentSlot() {
        return switch (this) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            case WEAPON -> EquipmentSlot.MAINHAND;
            case OFFHAND -> EquipmentSlot.OFFHAND;
        };
    }

    public Component label() {
        return Component.translatable("screen.rpgmechanics.gear.slot." + name().toLowerCase(Locale.ROOT));
    }

    public static GearSlot fromOrdinalSafe(int ordinal) {
        GearSlot[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    public static List<GearSlot> armorAndHands() {
        return List.of(values());
    }
}
