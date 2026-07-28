package com.sigils.core.draft;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.ModifierOp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GlyphAvailabilityTest {

    private static Glyph glyphOfComplexity(int complexity) {
        return new Glyph(
                "test:mod", GlyphRole.MODIFIER,
                List.of(StrokePath.of(new Vec2(0f, 0.5f), new Vec2(1f, 0.5f))),
                0.05f, complexity, 1f,
                Optional.empty(), Optional.of(new ModifierOp.Scale(1.5f)));
    }

    private static DraftLimits penOfComplexity(int maxComplexity) {
        return new DraftLimits(8, 1, maxComplexity, 0.45f, false);
    }

    @Test
    @DisplayName("a simple glyph in a capable pen is available")
    void simpleGlyphIsAvailable() {
        GlyphAvailability availability =
                GlyphAvailability.of(glyphOfComplexity(1), penOfComplexity(5));

        assertTrue(availability.allowed());
        assertEquals(GlyphAvailability.Reason.AVAILABLE, availability.reason());
    }

    @Test
    @DisplayName("exactly at the limit is still allowed")
    void boundaryIsInclusive() {
        assertTrue(GlyphAvailability.of(glyphOfComplexity(4), penOfComplexity(4)).allowed());
    }

    @Test
    @DisplayName("too intricate reports both numbers, so the message can name them")
    void tooComplexCarriesTheNumbers() {
        GlyphAvailability availability =
                GlyphAvailability.of(glyphOfComplexity(4), penOfComplexity(2));

        assertFalse(availability.allowed());
        assertEquals(GlyphAvailability.Reason.TOO_COMPLEX, availability.reason());
        assertEquals(4, availability.required());
        assertEquals(2, availability.available());
    }

    @Test
    @DisplayName("allowed() and the reason cannot disagree")
    void allowedTracksReason() {
        assertTrue(GlyphAvailability.OK.allowed());
        assertFalse(new GlyphAvailability(GlyphAvailability.Reason.NOT_LEARNED, 0, 0).allowed());
    }
}