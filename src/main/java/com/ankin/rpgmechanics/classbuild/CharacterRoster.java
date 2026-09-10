package com.ankin.rpgmechanics.classbuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Three Destiny-style character slots plus which one is currently played.
 */
public record CharacterRoster(List<CharacterSlot> slots, int activeSlot) {
    public static final int SLOT_COUNT = 3;
    public static final int NONE = -1;

    public static final CharacterRoster EMPTY = new CharacterRoster(emptySlots(), NONE);

    private static final Codec<CharacterRoster> NATIVE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CharacterSlot.CODEC.listOf().fieldOf("slots").forGetter(CharacterRoster::slots),
            Codec.INT.optionalFieldOf("activeSlot", NONE).forGetter(CharacterRoster::activeSlot)
    ).apply(instance, CharacterRoster::normalize));

    /**
     * Native roster requires {@code slots}. Legacy single {@link ClassBuildState} is tried only if that fails
     * (all ClassBuildState fields are optional, so it must not be first or it swallows roster data).
     */
    public static final Codec<CharacterRoster> CODEC = Codec.withAlternative(
            NATIVE_CODEC,
            ClassBuildState.CODEC.xmap(CharacterRoster::fromLegacy, roster -> roster.activeBuild().orElse(ClassBuildState.EMPTY))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterRoster> STREAM_CODEC = StreamCodec.composite(
            CharacterSlot.STREAM_CODEC.apply(ByteBufCodecs.list(SLOT_COUNT)),
            CharacterRoster::slots,
            ByteBufCodecs.VAR_INT,
            CharacterRoster::activeSlot,
            CharacterRoster::normalize
    );

    public CharacterRoster {
        slots = normalizeSlots(slots);
        if (activeSlot < NONE || activeSlot >= SLOT_COUNT) {
            activeSlot = NONE;
        }
        if (activeSlot >= 0 && !slots.get(activeSlot).occupied()) {
            activeSlot = NONE;
        }
    }

    public static CharacterRoster normalize(List<CharacterSlot> slots, int activeSlot) {
        return new CharacterRoster(slots, activeSlot);
    }

    public static List<CharacterSlot> emptySlots() {
        List<CharacterSlot> list = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            list.add(CharacterSlot.EMPTY);
        }
        return list;
    }

    public static CharacterRoster fromLegacy(ClassBuildState state) {
        if (state == null || !state.confirmed()) {
            return EMPTY;
        }
        List<CharacterSlot> slots = emptySlots();
        slots.set(0, CharacterSlot.create("Guardian", state));
        return new CharacterRoster(slots, 0);
    }

    public boolean hasActive() {
        return activeSlot >= 0 && activeSlot < SLOT_COUNT && slots.get(activeSlot).occupied();
    }

    public Optional<ClassBuildState> activeBuild() {
        if (!hasActive()) {
            return Optional.empty();
        }
        return Optional.of(slots.get(activeSlot).build());
    }

    public ClassBuildState activeBuildOrEmpty() {
        return activeBuild().orElse(ClassBuildState.EMPTY);
    }

    public Optional<CharacterSlot> activeCharacter() {
        if (!hasActive()) {
            return Optional.empty();
        }
        return Optional.of(slots.get(activeSlot));
    }

    public CharacterSlot slot(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            return CharacterSlot.EMPTY;
        }
        return slots.get(index);
    }

    public CharacterRoster withSlot(int index, CharacterSlot slot) {
        List<CharacterSlot> next = new ArrayList<>(normalizeSlots(slots));
        next.set(index, slot == null ? CharacterSlot.EMPTY : slot);
        return new CharacterRoster(next, activeSlot);
    }

    public CharacterRoster withActive(int index) {
        return new CharacterRoster(slots, index);
    }

    public CharacterRoster clearActive() {
        return new CharacterRoster(slots, NONE);
    }

    private static List<CharacterSlot> normalizeSlots(List<CharacterSlot> incoming) {
        List<CharacterSlot> list = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (incoming != null && i < incoming.size() && incoming.get(i) != null) {
                list.add(incoming.get(i));
            } else {
                list.add(CharacterSlot.EMPTY);
            }
        }
        return List.copyOf(list);
    }
}
