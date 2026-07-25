package com.sigils.core.trace;

/**
 * The outcome of comparing a player's trace against a glyph's ideal strokes.
 *
 * @param valid         did the trace clear the coverage and tolerance gates?
 * @param fidelity      0..1 quality of a valid trace; 0 if invalid. Drives
 *                      instability at cast time (higher fidelity, calmer spell).
 * @param coverage      fraction of the ideal geometry the trace visited (0..1)
 * @param meanDeviation average distance of traced points from the ideal stroke
 * @param maxDeviation  worst single deviation, for diagnostics
 */
public record TraceResult(
        boolean valid,
        float fidelity,
        float coverage,
        float meanDeviation,
        float maxDeviation
) {
    /** A trace that could not be evaluated (empty input, zero tolerance, etc.). */
    public static final TraceResult FAILED =
            new TraceResult(false, 0f, 0f, Float.MAX_VALUE, Float.MAX_VALUE);
}