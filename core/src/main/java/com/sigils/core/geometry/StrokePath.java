package com.sigils.core.geometry;

import java.util.List;
import java.util.Objects;

/** A single continuous stroke: an ordered polyline of at least two points. */
public record StrokePath(List<Vec2> points) {

    public StrokePath {
        Objects.requireNonNull(points, "points");
        if (points.size() < 2) {
            throw new IllegalArgumentException("A stroke path needs at least 2 points");
        }
        points = List.copyOf(points);
    }

    public static StrokePath of(Vec2... pts) {
        return new StrokePath(List.of(pts));
    }

    /** Total arc length along the polyline. */
    public float length() {
        float total = 0f;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).distanceTo(points.get(i));
        }
        return total;
    }

    /** Minimum distance from {@code p} to any segment of this stroke. */
    public float distanceTo(Vec2 p) {
        float best = Float.MAX_VALUE;
        for (int i = 1; i < points.size(); i++) {
            best = Math.min(best, distanceToSegment(p, points.get(i - 1), points.get(i)));
        }
        return best;
    }

    /** The point at fraction {@code t} (0..1) along the stroke, by arc length. */
    public Vec2 pointAtFraction(float t) {
        float target = length() * Math.clamp(t, 0f, 1f);
        float travelled = 0f;
        for (int i = 1; i < points.size(); i++) {
            Vec2 a = points.get(i - 1);
            Vec2 b = points.get(i);
            float seg = a.distanceTo(b);
            if (travelled + seg >= target || i == points.size() - 1) {
                float local = seg <= 0f ? 0f : (target - travelled) / seg;
                return a.plus(b.minus(a).scaled(Math.clamp(local, 0f, 1f)));
            }
            travelled += seg;
        }
        return points.get(points.size() - 1);
    }

    /** Distance from point {@code p} to segment {@code a→b}, clamping to the endpoints. */
    private static float distanceToSegment(Vec2 p, Vec2 a, Vec2 b) {
        Vec2 ab = b.minus(a);
        float abLenSq = ab.lengthSquared();
        if (abLenSq <= 1e-12f) {
            return p.distanceTo(a); // degenerate segment
        }
        float t = Math.clamp(p.minus(a).dot(ab) / abLenSq, 0f, 1f);
        Vec2 closest = a.plus(ab.scaled(t));
        return p.distanceTo(closest);
    }
}