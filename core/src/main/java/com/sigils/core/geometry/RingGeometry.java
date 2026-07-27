package com.sigils.core.geometry;

import java.util.List;

/**
 * Questions you can only ask of a closed loop: did it close, and what does it
 * contain?
 *
 * <p>The ring is what makes a circle castable, so "your ring has a gap in it"
 * has to be a specific, checkable failure rather than a vague rejection.
 */
public final class RingGeometry {

    /** Fewer points than this and it isn't a loop, it's a scribble. */
    public static final int MIN_LOOP_POINTS = 8;

    private RingGeometry() {}

    /** Distance between the first and last point of a trace — the gap in the ring. */
    public static float closureGap(List<Vec2> trace) {
        if (trace.size() < 2) {
            return Float.MAX_VALUE;
        }
        return trace.get(0).distanceTo(trace.get(trace.size() - 1));
    }

    /** True when the trace has enough points and its ends meet within {@code tolerance}. */
    public static boolean isClosed(List<Vec2> trace, float tolerance) {
        return trace.size() >= MIN_LOOP_POINTS && closureGap(trace) <= tolerance;
    }

    /**
     * Even-odd point-in-polygon test: cast a ray and count crossings.
     *
     * <p>The loop is treated as implicitly closed (last point joins first), so a
     * trace that ended a hair short of its start still encloses correctly.
     */
    public static boolean encloses(List<Vec2> loop, Vec2 point) {
        if (loop.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = loop.size() - 1; i < loop.size(); j = i++) {
            Vec2 a = loop.get(i);
            Vec2 b = loop.get(j);
            boolean straddles = (a.y() > point.y()) != (b.y() > point.y());
            if (straddles) {
                float crossingX = (b.x() - a.x()) * (point.y() - a.y()) / (b.y() - a.y()) + a.x();
                if (point.x() < crossingX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}