package com.ankin.rpgmechanics.classbuild;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Per-player class / loadout state. Source of truth for the managed Iron Spells book.
 */
public record ClassBuildState(
        boolean confirmed,
        ClassRole role,
        ElementTrack track,
        ResourceLocation melee,
        ResourceLocation movement,
        ResourceLocation ranged,
        ResourceLocation ultimate
) {
    public static final ClassBuildState EMPTY = new ClassBuildState(
            false,
            ClassRole.NONE,
            ElementTrack.NONE,
            ClassBuildCatalog.DEFAULT_MELEE,
            ClassBuildCatalog.DEFAULT_MOVEMENT,
            ClassBuildCatalog.DEFAULT_RANGED,
            ClassBuildCatalog.DEFAULT_ULTIMATE
    );

    public static final Codec<ClassBuildState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("confirmed", false).forGetter(ClassBuildState::confirmed),
            ClassRole.CODEC.optionalFieldOf("role", ClassRole.NONE).forGetter(ClassBuildState::role),
            ElementTrack.CODEC.optionalFieldOf("track", ElementTrack.NONE).forGetter(ClassBuildState::track),
            ResourceLocation.CODEC.optionalFieldOf("melee", ClassBuildCatalog.DEFAULT_MELEE).forGetter(ClassBuildState::melee),
            ResourceLocation.CODEC.optionalFieldOf("movement", ClassBuildCatalog.DEFAULT_MOVEMENT).forGetter(ClassBuildState::movement),
            ResourceLocation.CODEC.optionalFieldOf("ranged", ClassBuildCatalog.DEFAULT_RANGED).forGetter(ClassBuildState::ranged),
            ResourceLocation.CODEC.optionalFieldOf("ultimate", ClassBuildCatalog.DEFAULT_ULTIMATE).forGetter(ClassBuildState::ultimate)
    ).apply(instance, ClassBuildState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassBuildState> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                buf.writeBoolean(state.confirmed());
                buf.writeEnum(state.role());
                buf.writeEnum(state.track());
                ResourceLocation.STREAM_CODEC.encode(buf, state.melee());
                ResourceLocation.STREAM_CODEC.encode(buf, state.movement());
                ResourceLocation.STREAM_CODEC.encode(buf, state.ranged());
                ResourceLocation.STREAM_CODEC.encode(buf, state.ultimate());
            },
            buf -> new ClassBuildState(
                    buf.readBoolean(),
                    buf.readEnum(ClassRole.class),
                    buf.readEnum(ElementTrack.class),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf)
            )
    );

    public List<ResourceLocation> orderedSpells() {
        return List.of(melee, movement, ranged, ultimate);
    }

    public Optional<String> validationError() {
        if (role != ClassRole.DAMAGE_DEALER) {
            return Optional.of("Only Damage Dealer is available");
        }
        if (track != ElementTrack.DARKNESS) {
            return Optional.of("Only Darkness track is available");
        }
        if (!ClassBuildCatalog.MELEE.contains(melee)) {
            return Optional.of("Invalid melee ability");
        }
        if (!ClassBuildCatalog.MOVEMENT.contains(movement)) {
            return Optional.of("Invalid movement ability");
        }
        if (!ClassBuildCatalog.RANGED.contains(ranged)) {
            return Optional.of("Invalid ranged ability");
        }
        if (!ClassBuildCatalog.ULTIMATE.contains(ultimate)) {
            return Optional.of("Invalid ultimate");
        }
        return Optional.empty();
    }

    public ClassBuildState confirmedCopy() {
        return new ClassBuildState(true, role, track, melee, movement, ranged, ultimate);
    }

    public enum ClassRole implements StringRepresentable {
        NONE("none"),
        DAMAGE_DEALER("damage_dealer"),
        TANK("tank"),
        SUPPORT("support");

        public static final Codec<ClassRole> CODEC = StringRepresentable.fromEnum(ClassRole::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClassRole> STREAM_CODEC =
                StreamCodec.of(
                        (buf, value) -> buf.writeEnum(value),
                        buf -> buf.readEnum(ClassRole.class)
                );

        private final String id;

        ClassRole(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public enum ElementTrack implements StringRepresentable {
        NONE("none"),
        DARKNESS("darkness"),
        RELIGIOUS("religious"),
        ELEMENTAL("elemental");

        public static final Codec<ElementTrack> CODEC = StringRepresentable.fromEnum(ElementTrack::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, ElementTrack> STREAM_CODEC =
                StreamCodec.of(
                        (buf, value) -> buf.writeEnum(value),
                        buf -> buf.readEnum(ElementTrack.class)
                );

        private final String id;

        ElementTrack(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
