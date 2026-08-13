package com.ankin.rpgmechanics.world.border;

import java.util.Optional;

import com.ankin.rpgmechanics.config.RpgMechanicsConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Active border for one dimension (custom polygon or vanilla-mimic default).
 */
public record WorldBorderDefinition(
        ResourceLocation dimension,
        boolean enabled,
        boolean custom,
        BorderPolygon polygon,
        double fogDepth,
        double softMargin,
        double maxDamagePerSecond
) {
    public static final Codec<WorldBorderDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(WorldBorderDefinition::dimension),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(WorldBorderDefinition::enabled),
            Codec.BOOL.optionalFieldOf("custom", true).forGetter(WorldBorderDefinition::custom),
            BorderPolygon.CODEC.fieldOf("polygon").forGetter(WorldBorderDefinition::polygon),
            Codec.DOUBLE.optionalFieldOf("fogDepth").forGetter(d -> Optional.of(d.fogDepth())),
            Codec.DOUBLE.optionalFieldOf("softMargin").forGetter(d -> Optional.of(d.softMargin())),
            Codec.DOUBLE.optionalFieldOf("maxDamagePerSecond").forGetter(d -> Optional.of(d.maxDamagePerSecond()))
    ).apply(instance, (dimension, enabled, custom, polygon, fogDepth, softMargin, maxDamage) ->
            new WorldBorderDefinition(
                    dimension,
                    enabled,
                    custom,
                    polygon,
                    fogDepth.orElse(defaultFogDepth()),
                    softMargin.orElse(defaultSoftMargin()),
                    maxDamage.orElse(defaultMaxDamage())
            )));

    /** Codec for on-disk JSON that stores vertices at the top level. */
    public static final Codec<WorldBorderDefinition> FILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(WorldBorderDefinition::dimension),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(WorldBorderDefinition::enabled),
            BorderPolygon.Vertex.CODEC.listOf().fieldOf("vertices").forGetter(d -> d.polygon().vertices()),
            Codec.DOUBLE.optionalFieldOf("fogDepth").forGetter(d -> Optional.of(d.fogDepth())),
            Codec.DOUBLE.optionalFieldOf("softMargin").forGetter(d -> Optional.of(d.softMargin())),
            Codec.DOUBLE.optionalFieldOf("maxDamagePerSecond").forGetter(d -> Optional.of(d.maxDamagePerSecond()))
    ).apply(instance, (dimension, enabled, vertices, fogDepth, softMargin, maxDamage) ->
            new WorldBorderDefinition(
                    dimension,
                    enabled,
                    true,
                    new BorderPolygon(vertices),
                    fogDepth.orElse(defaultFogDepth()),
                    softMargin.orElse(defaultSoftMargin()),
                    maxDamage.orElse(defaultMaxDamage())
            )));

    public static WorldBorderDefinition vanillaMimic(ResourceLocation dimension) {
        return new WorldBorderDefinition(
                dimension,
                true,
                false,
                BorderPolygon.vanillaMimicSquare(),
                defaultFogDepth(),
                defaultSoftMargin(),
                defaultMaxDamage()
        );
    }

    public static double defaultFogDepth() {
        return readDouble(() -> RpgMechanicsConfig.SERVER.borderFogDepth.get(), 32.0);
    }

    public static double defaultSoftMargin() {
        return readDouble(() -> RpgMechanicsConfig.SERVER.borderSoftMargin.get(), 8.0);
    }

    public static double defaultMaxDamage() {
        return readDouble(() -> RpgMechanicsConfig.SERVER.borderMaxDamagePerSecond.get(), 4.0);
    }

    private static double readDouble(DoubleSupplier supplier, double fallback) {
        if (!RpgMechanicsConfig.SERVER_SPEC.isLoaded()) {
            return fallback;
        }
        try {
            return supplier.getAsDouble();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }

    @FunctionalInterface
    private interface DoubleSupplier {
        double getAsDouble();
    }
}
