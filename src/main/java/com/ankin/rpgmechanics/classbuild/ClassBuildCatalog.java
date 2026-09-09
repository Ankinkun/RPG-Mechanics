package com.ankin.rpgmechanics.classbuild;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * Darkness Damage Dealer ability catalog + ultimate → spellbook mapping.
 */
public final class ClassBuildCatalog {
    public static final ResourceLocation DEFAULT_MELEE = iss("echoing_strikes");
    public static final ResourceLocation DEFAULT_MOVEMENT = iss("blood_step");
    public static final ResourceLocation DEFAULT_RANGED = iss("blood_slash");
    public static final ResourceLocation DEFAULT_ULTIMATE = iss("eldritch_blast");

    public static final List<ResourceLocation> MELEE = List.of(
            iss("echoing_strikes"),
            iss("devour")
    );

    public static final List<ResourceLocation> MOVEMENT = List.of(
            iss("abyssal_shroud"),
            iss("evasion"),
            iss("blood_step")
    );

    public static final List<ResourceLocation> RANGED = List.of(
            iss("sonic_boom"),
            iss("blood_slash"),
            iss("magic_arrow")
    );

    public static final List<ResourceLocation> ULTIMATE = List.of(
            iss("eldritch_blast"),
            iss("acupuncture"),
            iss("black_hole")
    );

    /** Ultimate spell → Iron Spells spellbook item. */
    public static final Map<ResourceLocation, ResourceLocation> ULTIMATE_BOOKS = Map.of(
            iss("black_hole"), iss("dragonskin_spell_book"),
            iss("acupuncture"), iss("cursed_doll_spell_book"),
            iss("eldritch_blast"), iss("netherite_spell_book")
    );

    private ClassBuildCatalog() {
    }

    public static ResourceLocation bookForUltimate(ResourceLocation ultimate) {
        return ULTIMATE_BOOKS.getOrDefault(ultimate, iss("netherite_spell_book"));
    }

    public static String displayName(ResourceLocation spellId) {
        String path = spellId.getPath().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String word : path.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static ResourceLocation iss(String path) {
        return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path);
    }
}
