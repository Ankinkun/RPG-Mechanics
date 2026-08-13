package com.ankin.rpgmechanics.world.border;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.border.WorldBorder;

/**
 * Simple closed XZ polygon. Inside = safe zone.
 */
public record BorderPolygon(List<Vertex> vertices) {
    /** Half of vanilla {@link WorldBorder#MAX_SIZE} (~29,999,984). */
    public static final double VANILLA_HALF_EXTENT = WorldBorder.MAX_SIZE / 2.0;

    public static final Codec<BorderPolygon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vertex.CODEC.listOf().fieldOf("vertices").forGetter(BorderPolygon::vertices)
    ).apply(instance, BorderPolygon::new));

    public BorderPolygon {
        vertices = List.copyOf(vertices == null ? List.of() : vertices);
    }

    public static BorderPolygon vanillaMimicSquare() {
        double h = VANILLA_HALF_EXTENT;
        return new BorderPolygon(List.of(
                new Vertex(-h, -h),
                new Vertex(h, -h),
                new Vertex(h, h),
                new Vertex(-h, h)
        ));
    }

    public boolean isValid() {
        return vertices.size() >= 3;
    }

    /**
     * True when the closed polygon has no proper edge crossings (simple polygon).
     * Adjacent edges that only share an endpoint are allowed.
     */
    public boolean isSimple() {
        return validationMessage().isEmpty();
    }

    /**
     * Human-readable problem, or empty if the closed polygon is usable.
     */
    public java.util.Optional<String> validationMessage() {
        int n = vertices.size();
        if (n < 3) {
            return java.util.Optional.of("Need at least 3 vertices to form an area");
        }
        for (int i = 0; i < n; i++) {
            Vertex a = vertices.get(i);
            Vertex b = vertices.get((i + 1) % n);
            if (a.x() == b.x() && a.z() == b.z()) {
                return java.util.Optional.of("Duplicate consecutive vertices at #" + (i + 1));
            }
        }
        // Proper intersections between non-adjacent edges (including closing edge).
        for (int i = 0; i < n; i++) {
            Vertex a1 = vertices.get(i);
            Vertex a2 = vertices.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                // Skip adjacent edges and the pair that shares the closing vertex.
                if (j == i || (j + 1) % n == i || (i + 1) % n == j) {
                    continue;
                }
                Vertex b1 = vertices.get(j);
                Vertex b2 = vertices.get((j + 1) % n);
                if (segmentsProperlyIntersect(a1.x(), a1.z(), a2.x(), a2.z(), b1.x(), b1.z(), b2.x(), b2.z())) {
                    return java.util.Optional.of(
                            "Self-intersecting sides (edges #" + (i + 1) + " and #" + (j + 1) + " cross)"
                    );
                }
            }
        }
        // Degenerate: almost zero area (all points nearly colinear).
        if (Math.abs(signedArea()) < 1.0E-3) {
            return java.util.Optional.of("Polygon has no area (vertices are colinear or collapsed)");
        }
        return java.util.Optional.empty();
    }

    public double signedArea() {
        double sum = 0.0;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            Vertex a = vertices.get(i);
            Vertex b = vertices.get((i + 1) % n);
            sum += a.x() * b.z() - b.x() * a.z();
        }
        return sum * 0.5;
    }

    private static boolean segmentsProperlyIntersect(
            double ax, double az, double bx, double bz,
            double cx, double cz, double dx, double dz
    ) {
        double d1 = cross(cx, cz, dx, dz, ax, az);
        double d2 = cross(cx, cz, dx, dz, bx, bz);
        double d3 = cross(ax, az, bx, bz, cx, cz);
        double d4 = cross(ax, az, bx, bz, dx, dz);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return false;
    }

    private static double cross(double ox, double oz, double ax, double az, double bx, double bz) {
        return (ax - ox) * (bz - oz) - (az - oz) * (bx - ox);
    }

    public boolean contains(double x, double z) {
        if (!isValid()) {
            return true;
        }
        // Ray cast eastward; count crossings of edges.
        boolean inside = false;
        int count = vertices.size();
        for (int i = 0, j = count - 1; i < count; j = i++) {
            Vertex vi = vertices.get(i);
            Vertex vj = vertices.get(j);
            boolean intersect = ((vi.z() > z) != (vj.z() > z))
                    && (x < (vj.x() - vi.x()) * (z - vi.z()) / (vj.z() - vi.z() + 1.0E-12) + vi.x());
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * @return 0 if inside; otherwise distance to nearest edge segment
     */
    public double distanceOutside(double x, double z) {
        if (!isValid() || contains(x, z)) {
            return 0.0;
        }
        return distanceToEdge(x, z);
    }

    /**
     * Absolute distance to the nearest edge segment (works inside or outside).
     */
    public double distanceToEdge(double x, double z) {
        if (!isValid()) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        int count = vertices.size();
        for (int i = 0; i < count; i++) {
            Vertex a = vertices.get(i);
            Vertex b = vertices.get((i + 1) % count);
            best = Math.min(best, distanceToSegment(x, z, a.x(), a.z(), b.x(), b.z()));
        }
        return best;
    }

    private static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        if (dx == 0.0 && dz == 0.0) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz);
        t = Math.max(0.0, Math.min(1.0, t));
        double qx = ax + t * dx;
        double qz = az + t * dz;
        return Math.hypot(px - qx, pz - qz);
    }

    public BorderPolygon withVertex(Vertex vertex) {
        List<Vertex> next = new ArrayList<>(vertices);
        next.add(vertex);
        return new BorderPolygon(next);
    }

    public BorderPolygon withoutLastVertex() {
        if (vertices.isEmpty()) {
            return this;
        }
        return new BorderPolygon(vertices.subList(0, vertices.size() - 1));
    }

    public record Vertex(double x, double z) {
        public static final Codec<Vertex> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Vertex::x),
                Codec.DOUBLE.fieldOf("z").forGetter(Vertex::z)
        ).apply(instance, Vertex::new));
    }
}
