package com.sigils.core.trace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceScoresTest {

    /** A valid result carrying just the fidelity this test cares about. */
    private static TraceResult scored(float fidelity) {
        return new TraceResult(true, fidelity, 1f, 0.01f, 0.02f);
    }

    @Test
    @DisplayName("a spell's fidelity is the mean of its glyphs'")
    void meanOfSeveral() {
        assertEquals(0.8f, TraceScores.mean(List.of(scored(1.0f), scored(0.8f), scored(0.6f))), 1e-5);
    }

    @Test
    @DisplayName("nothing traced is nothing earned")
    void emptyIsZero() {
        assertEquals(0f, TraceScores.mean(List.of()), 1e-5);
    }

    @Test
    @DisplayName("a failed trace contributes its zero, it isn't skipped")
    void failedResultsCountAsZero() {
        assertEquals(0.5f, TraceScores.mean(List.of(scored(1.0f), TraceResult.FAILED)), 1e-5);
    }
}