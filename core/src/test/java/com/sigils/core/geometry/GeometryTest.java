package com.sigils.core.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {

    @Test
    void vectorArithmetic() {
        Vec2 a = new Vec2(1f, 2f);
        Vec2 b = new Vec2(3f, 4f);
        assertEquals(new Vec2(4f, 6f), a.plus(b));
        assertEquals(new Vec2(-2f, -2f), a.minus(b));
        assertEquals(new Vec2(2f, 4f), a.scaled(2f));
        assertEquals(5f, new Vec2(3f, 4f).length(), 1e-5);
    }

    @Test
    void straightPathLength() {
        StrokePath path = StrokePath.of(new Vec2(0f, 0f), new Vec2(1f, 0f), new Vec2(1f, 1f));
        assertEquals(2f, path.length(), 1e-5); // 1 across + 1 up
    }

    @Test
    void distanceToSegmentClampsToEndpoints() {
        StrokePath line = StrokePath.of(new Vec2(0f, 0f), new Vec2(1f, 0f));

        // A point directly above the middle: distance is the vertical gap.
        assertEquals(0.2f, line.distanceTo(new Vec2(0.5f, 0.2f)), 1e-5);

        // A point beyond the end: distance is to the endpoint, not the infinite line.
        assertEquals(1f, line.distanceTo(new Vec2(2f, 0f)), 1e-5);

        // A point on the line: distance zero.
        assertEquals(0f, line.distanceTo(new Vec2(0.5f, 0f)), 1e-5);
    }

    @Test
    void pointAtFractionWalksArcLength() {
        StrokePath line = StrokePath.of(new Vec2(0f, 0f), new Vec2(1f, 0f));
        assertEquals(new Vec2(0f, 0f), line.pointAtFraction(0f));
        assertEquals(new Vec2(0.5f, 0f).x(), line.pointAtFraction(0.5f).x(), 1e-5);
        assertEquals(new Vec2(1f, 0f), line.pointAtFraction(1f));
    }
}