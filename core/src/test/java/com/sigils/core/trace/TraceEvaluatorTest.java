package com.sigils.core.trace;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceEvaluatorTest {

    private final TraceEvaluator evaluator = new TraceEvaluator();

    // The ideal glyph: a straight horizontal line from (0,0) to (1,0).
    private static final List<StrokePath> IDEAL =
            List.of(StrokePath.of(new Vec2(0f, 0f), new Vec2(1f, 0f)));

    private static final float TOL = 0.1f;

    // A trace of `count` points evenly along the line, offset vertically by `dy`.
    private static List<Vec2> traceAlongLine(int count, float dy, float fromX, float toX) {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float t = (float) i / (count - 1);
            pts.add(new Vec2(fromX + (toX - fromX) * t, dy));
        }
        return pts;
    }

    @Test
    @DisplayName("a perfect trace scores near-perfect fidelity and is valid")
    void perfectTrace() {
        TraceResult r = evaluator.evaluate(IDEAL, traceAlongLine(40, 0f, 0f, 1f), TOL);
        assertTrue(r.valid());
        assertTrue(r.fidelity() > 0.95f, "fidelity was " + r.fidelity());
        assertEquals(1f, r.coverage(), 1e-3);
    }

    @Test
    @DisplayName("a trace offset by half the tolerance is valid but lower fidelity")
    void slightlyOffTrace() {
        TraceResult r = evaluator.evaluate(IDEAL, traceAlongLine(40, TOL / 2f, 0f, 1f), TOL);
        assertTrue(r.valid());
        // meanDeviation ~ tol/2 => accuracy ~ 0.5, coverage ~ 1 => fidelity ~ 0.5
        assertEquals(0.5f, r.fidelity(), 0.1f);
    }

    @Test
    @DisplayName("a trace far outside the lines is invalid with zero fidelity")
    void wildlyOffTrace() {
        TraceResult r = evaluator.evaluate(IDEAL, traceAlongLine(40, TOL * 5f, 0f, 1f), TOL);
        assertFalse(r.valid());
        assertEquals(0f, r.fidelity(), 1e-5);
    }

    @Test
    @DisplayName("tracing only part of the glyph fails on coverage")
    void partialTrace() {
        // Perfectly on the line, but only across the first half.
        TraceResult r = evaluator.evaluate(IDEAL, traceAlongLine(40, 0f, 0f, 0.5f), TOL);
        assertFalse(r.valid(), "half-covered trace should be invalid");
        assertTrue(r.coverage() < 0.75f, "coverage was " + r.coverage());
    }

    @Test
    @DisplayName("empty input yields FAILED")
    void emptyTrace() {
        TraceResult r = evaluator.evaluate(IDEAL, List.of(), TOL);
        assertEquals(TraceResult.FAILED, r);
    }
}