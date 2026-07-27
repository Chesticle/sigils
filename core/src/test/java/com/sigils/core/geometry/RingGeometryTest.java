package com.sigils.core.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RingGeometryTest {

    /** A circle of {@code count} points, radius 0.4, centred on the canvas. */
    private static List<Vec2> circle(int count, boolean close) {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = 2 * Math.PI * i / count;
            pts.add(new Vec2(0.5f + 0.4f * (float) Math.cos(a), 0.5f + 0.4f * (float) Math.sin(a)));
        }
        if (close) {
            pts.add(pts.get(0));
        }
        return pts;
    }

    @Test
    @DisplayName("a loop whose ends meet is closed")
    void closedLoop() {
        assertTrue(RingGeometry.isClosed(circle(32, true), 0.05f));
    }

    @Test
    @DisplayName("a loop stopped three-quarters of the way round is not closed")
    void gappedLoop() {
        List<Vec2> partial = circle(32, false).subList(0, 24);
        assertFalse(RingGeometry.isClosed(partial, 0.05f));
        assertTrue(RingGeometry.closureGap(partial) > 0.05f);
    }

    @Test
    @DisplayName("a glyph at the centre is inside the ring")
    void centreIsEnclosed() {
        assertTrue(RingGeometry.encloses(circle(32, true), new Vec2(0.5f, 0.5f)));
    }

    @Test
    @DisplayName("a glyph pushed past the edge is outside the ring")
    void outsideIsNotEnclosed() {
        assertFalse(RingGeometry.encloses(circle(32, true), new Vec2(0.97f, 0.5f)));
    }
}