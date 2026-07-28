package com.sigils.core.trace;

import java.util.Collection;

/** How per-glyph trace results combine into one number for the whole spell. */
public final class TraceScores {

    private TraceScores() {}

    /**
     * The spell's fidelity: the mean of its glyphs'.
     *
     * <p>A plain average is deliberate — a spell is only as good as its
     * handwriting overall, and weighting by glyph size or role would make a
     * sloppy ring hideable behind three neat modifiers.
     */
    public static float mean(Collection<TraceResult> results) {
        if (results.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        for (TraceResult result : results) {
            sum += result.fidelity();
        }
        return sum / results.size();
    }
}