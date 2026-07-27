package com.sigils.core.draft;

import com.sigils.core.geometry.Vec2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrokeQuantizerTest {

    @Test
    @DisplayName("a round trip loses less than one quantisation step")
    void roundTripIsAccurate() {
        List<Vec2> original = List.of(
                new Vec2(0f, 0f), new Vec2(0.333f, 0.667f),
                new Vec2(0.5f, 0.5f), new Vec2(1f, 1f));

        List<Vec2> back = StrokeQuantizer.decode(StrokeQuantizer.encode(original));

        assertEquals(original.size(), back.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).x(), back.get(i).x(), 1f / 255f);
            assertEquals(original.get(i).y(), back.get(i).y(), 1f / 255f);
        }
    }

    @Test
    @DisplayName("points outside the canvas are clamped, never wrapped")
    void outOfRangeClamps() {
        List<Vec2> back = StrokeQuantizer.decode(
                StrokeQuantizer.encode(List.of(new Vec2(-3f, 4f))));

        assertEquals(0f, back.get(0).x(), 1e-5);
        assertEquals(1f, back.get(0).y(), 1e-5);
    }

    @Test
    @DisplayName("an oversized stroke is rejected before anything is allocated")
    void oversizedStrokeRejected() {
        List<Vec2> huge = new ArrayList<>();
        for (int i = 0; i <= StrokeQuantizer.MAX_POINTS_PER_STROKE; i++) {
            huge.add(new Vec2(0.5f, 0.5f));
        }
        assertThrows(IllegalArgumentException.class, () -> StrokeQuantizer.encode(huge));
    }
}