package com.sigils.core.trace;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;

import java.util.List;

/**
 * Scores how faithfully a player's trace follows a glyph's ideal strokes.
 *
 * <p>Evaluates against a <em>list</em> of strokes so multi-stroke glyphs work:
 * a traced point's deviation is its distance to the nearest stroke, and
 * coverage is sampled across every stroke.
 */
public final class TraceEvaluator {

    /** How many points to sample along each ideal stroke when measuring coverage. */
    private static final int COVERAGE_SAMPLES = 64;
    /** A trace must cover at least this fraction of the ideal geometry. */
    private static final float COVERAGE_THRESHOLD = 0.85f;
    /** At least this fraction of traced points must sit within the tolerance band. */
    private static final float WITHIN_THRESHOLD = 0.80f;

    public TraceResult evaluate(List<StrokePath> ideal, List<Vec2> trace, float toleranceBand) {
        if (ideal.isEmpty() || trace.isEmpty() || toleranceBand <= 0f) {
            return TraceResult.FAILED;
        }

        // --- Accuracy: how far each traced point strays from the nearest stroke.
        float sumDeviation = 0f;
        float maxDeviation = 0f;
        int within = 0;
        for (Vec2 p : trace) {
            float d = nearestStrokeDistance(ideal, p);
            sumDeviation += d;
            maxDeviation = Math.max(maxDeviation, d);
            if (d <= toleranceBand) {
                within++;
            }
        }
        float meanDeviation = sumDeviation / trace.size();
        float withinFraction = (float) within / trace.size();

        // --- Coverage: how much of the ideal geometry the trace actually visited.
        float coverage = coverage(ideal, trace, toleranceBand);

        float accuracy = Math.clamp(1f - meanDeviation / toleranceBand, 0f, 1f);
        boolean valid = coverage >= COVERAGE_THRESHOLD && withinFraction >= WITHIN_THRESHOLD;
        float fidelity = valid ? accuracy * coverage : 0f;

        return new TraceResult(valid, fidelity, coverage, meanDeviation, maxDeviation);
    }

    private static float nearestStrokeDistance(List<StrokePath> ideal, Vec2 p) {
        float best = Float.MAX_VALUE;
        for (StrokePath path : ideal) {
            best = Math.min(best, path.distanceTo(p));
        }
        return best;
    }

    private static float coverage(List<StrokePath> ideal, List<Vec2> trace, float tolerance) {
        int covered = 0;
        int total = 0;
        for (StrokePath path : ideal) {
            for (int i = 0; i < COVERAGE_SAMPLES; i++) {
                float t = (float) i / (COVERAGE_SAMPLES - 1);
                Vec2 sample = path.pointAtFraction(t);
                total++;
                if (nearestTraceDistance(trace, sample) <= tolerance) {
                    covered++;
                }
            }
        }
        return total == 0 ? 0f : (float) covered / total;
    }

    private static float nearestTraceDistance(List<Vec2> trace, Vec2 sample) {
        float best = Float.MAX_VALUE;
        for (Vec2 p : trace) {
            best = Math.min(best, p.distanceTo(sample));
        }
        return best;
    }
}