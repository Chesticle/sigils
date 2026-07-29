package com.sigils.core.draft;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.ModifierOp;
import com.sigils.core.knowledge.KnownGlyphs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlyphAvailabilityTest {

    private static final String ID = "test:mod";

    /** A player who knows the glyph these tests build. */
    private static final KnownGlyphs KNOWN = new KnownGlyphs(Set.of(ID));

    private static Glyph glyphOfComplexity(int complexity) {
        return new Glyph(
                ID, GlyphRole.MODIFIER,
                List.of(StrokePath.of(new Vec2(0f, 0.5f), new Vec2(1f, 0.5f))),
                0.05f, complexity, 1f,
                Optional.empty(), Optional.of(new ModifierOp.Scale(1.5f)));
    }

    private static DraftLimits penOfComplexity(int maxComplexity) {
        return new DraftLimits(8, 1, maxComplexity, 0.45f, false);
    }

    @Test
    @DisplayName("a known, simple glyph in a capable pen is available")
    void simpleGlyphIsAvailable() {
        GlyphAvailability availability = GlyphAvailability.of(
                glyphOfComplexity(1), penOfComplexity(5), true, KNOWN);

        assertTrue(availability.allowed());
        assertEquals(GlyphAvailability.Reason.AVAILABLE, availability.reason());
    }

    @Test
    @DisplayName("exactly at the limit is still allowed")
    void boundaryIsInclusive() {
        assertTrue(GlyphAvailability.of(
                glyphOfComplexity(4), penOfComplexity(4), true, KNOWN).allowed());
    }

    @Test
    @DisplayName("too intricate reports both numbers, so the message can name them")
    void tooComplexCarriesTheNumbers() {
        GlyphAvailability availability = GlyphAvailability.of(
                glyphOfComplexity(4), penOfComplexity(2), true, KNOWN);

        assertFalse(availability.allowed());
        assertEquals(GlyphAvailability.Reason.TOO_COMPLEX, availability.reason());
        assertEquals(4, availability.required());
        assertEquals(2, availability.available());
    }

    @Test
    @DisplayName("a glyph the player has never learned is locked")
    void unlearnedIsLocked() {
        GlyphAvailability availability = GlyphAvailability.of(
                glyphOfComplexity(1), penOfComplexity(5), true, KnownGlyphs.NONE);

        assertFalse(availability.allowed());
        assertEquals(GlyphAvailability.Reason.NOT_LEARNED, availability.reason());
    }

    @Test
    @DisplayName("not learned beats too complex — the pen's limits are not a spoiler")
    void notLearnedWinsOverComplexity() {
        GlyphAvailability availability = GlyphAvailability.of(
                glyphOfComplexity(9), penOfComplexity(1), true, KnownGlyphs.NONE);

        assertEquals(GlyphAvailability.Reason.NOT_LEARNED, availability.reason());
        assertEquals(0, availability.required());
    }

    @Test
    @DisplayName("no pen beats everything")
    void noPenWinsOverNotLearned() {
        GlyphAvailability availability = GlyphAvailability.of(
                glyphOfComplexity(9), penOfComplexity(1), false, KnownGlyphs.NONE);

        assertEquals(GlyphAvailability.Reason.NO_PEN, availability.reason());
    }

    @Test
    @DisplayName("allowed() and the reason cannot disagree")
    void allowedTracksReason() {
        assertTrue(GlyphAvailability.OK.allowed());
        assertFalse(GlyphAvailability.NOT_LEARNED.allowed());
        assertFalse(GlyphAvailability.NO_PEN.allowed());
    }
}