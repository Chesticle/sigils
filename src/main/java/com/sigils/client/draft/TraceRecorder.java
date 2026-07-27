package com.sigils.client.draft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sigils.core.draft.InkCost;
import com.sigils.core.draft.InkLedger;
import com.sigils.core.draft.StrokeQuantizer;
import com.sigils.core.geometry.RingGeometry;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.GlyphTransform;
import com.sigils.core.trace.TraceEvaluator;
import com.sigils.core.trace.TraceResult;

/**
 * Records the player's pen strokes for one drafting session.
 *
 * <p>Everything here is a <em>prediction</em>. The samples are the only output
 * that leaves this class for the server; the scores it computes are for showing
 * the player how they're doing, and the server throws them away and recomputes.
 */
public final class TraceRecorder {

    /**
     * Minimum distance between samples, in canvas units.
     *
     * <p>Tied to {@link StrokeQuantizer#MAX_POINTS_PER_STROKE}: a full ring is
     * about 2.8 canvas units around, so at this spacing it lands near 190
     * points — inside the 256 cap with headroom. Sample more finely and a
     * careful player would hit the network limit mid-circle.
     */
    private static final float MIN_SAMPLE_DISTANCE = 0.015f;

    private final List<GlyphInstance> ordered = new ArrayList<>();
    private final List<List<StrokePath>> ideals = new ArrayList<>();
    private final List<Float> tolerances = new ArrayList<>();
    private final List<Float> idealLengths = new ArrayList<>();
    private final List<Float> costs = new ArrayList<>();
    private final List<List<Vec2>> samples = new ArrayList<>();
    private final List<TraceResult> results = new ArrayList<>();

    private final TraceEvaluator evaluator = new TraceEvaluator();
    private final InkLedger ledger;

    private int target;
    private boolean penDown;
    private String message = "";

    public TraceRecorder(List<GlyphInstance> placements, GlyphLookup glyphs, float inkCapacity) {
        this.ledger = new InkLedger(inkCapacity);

        List<GlyphInstance> sorted = new ArrayList<>(placements);
        sorted.sort(Comparator.comparingInt(placement -> traceOrder(placement, glyphs)));

        for (GlyphInstance placement : sorted) {
            Glyph glyph = glyphs.get(placement.glyphId()).orElse(null);
            if (glyph == null) {
                continue; // DraftValidator already refused this; skip defensively
            }
            List<StrokePath> ideal = GlyphTransform.toCanvas(glyph, placement);
            ordered.add(placement);
            ideals.add(ideal);
            tolerances.add(GlyphTransform.toleranceFor(glyph, placement));
            idealLengths.add(totalLength(ideal));
            costs.add(InkCost.of(placement, glyphs));
            samples.add(new ArrayList<>());
            results.add(null);
        }
    }

    private static int traceOrder(GlyphInstance placement, GlyphLookup glyphs) {
        GlyphRole role = glyphs.get(placement.glyphId()).map(Glyph::role).orElse(GlyphRole.MODIFIER);
        return switch (role) {
            case RING -> 0;
            case CREST -> 1;
            default -> 2;
        };
    }

    // ------------------------------------------------------------------ state

    public List<GlyphInstance> ordered() {
        return List.copyOf(ordered);
    }

    public int size() {
        return ordered.size();
    }

    public int target() {
        return target;
    }

    public boolean complete() {
        return target >= ordered.size();
    }

    public InkLedger ledger() {
        return ledger;
    }

    public String message() {
        return message;
    }

    public List<StrokePath> idealAt(int index) {
        return ideals.get(index);
    }

    public float toleranceAt(int index) {
        return tolerances.get(index);
    }

    public List<Vec2> samplesAt(int index) {
        return samples.get(index);
    }

    public boolean donePast(int index) {
        return results.get(index) != null;
    }

    /** Coverage of the glyph currently being traced, for the progress readout. */
    public float currentCoverage() {
        if (complete() || samples.get(target).isEmpty()) {
            return 0f;
        }
        return evaluator.evaluate(ideals.get(target), samples.get(target), tolerances.get(target))
                .coverage();
    }

    /** Distance from a canvas point to the current glyph's ideal strokes. */
    public float deviationAt(Vec2 point) {
        if (complete()) {
            return Float.MAX_VALUE;
        }
        float best = Float.MAX_VALUE;
        for (StrokePath stroke : ideals.get(target)) {
            best = Math.min(best, stroke.distanceTo(point));
        }
        return best;
    }

    /** The quantised samples, in the same order as {@link #ordered()}. */
    public List<byte[]> encoded() {
        List<byte[]> out = new ArrayList<>(samples.size());
        for (List<Vec2> points : samples) {
            out.add(StrokeQuantizer.encode(points));
        }
        return out;
    }

    /** Local scores, for display only — the server computes its own. */
    public Map<GlyphInstance, TraceResult> localResults() {
        Map<GlyphInstance, TraceResult> map = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            if (results.get(i) != null) {
                map.put(ordered.get(i), results.get(i));
            }
        }
        return map;
    }

    // -------------------------------------------------------------------- pen

    public void penDown(Vec2 point) {
        if (complete() || ledger.dry()) {
            return;
        }
        penDown = true;
        samples.get(target).add(point);
    }

    public void penMove(Vec2 point) {
        if (!penDown || complete()) {
            return;
        }
        List<Vec2> current = samples.get(target);
        if (current.size() >= StrokeQuantizer.MAX_POINTS_PER_STROKE) {
            penUp();
            message = "That trace is as long as the pen can record.";
            return;
        }

        Vec2 last = current.get(current.size() - 1);
        float travelled = last.distanceTo(point);
        if (travelled < MIN_SAMPLE_DISTANCE) {
            return;
        }

        // Ink is spent by distance drawn, scaled so that tracing a whole glyph
        // costs exactly the glyph's ink cost. Running dry aborts the stroke.
        float length = idealLengths.get(target);
        float cost = length <= 0f ? 0f : costs.get(target) * (travelled / length);
        if (!ledger.charge(cost)) {
            penDown = false;
            message = "The pen has run dry.";
            return;
        }

        current.add(point);
    }

    public void penUp() {
        if (!penDown || complete()) {
            return;
        }
        penDown = false;
        evaluateTarget();
    }

    /** Throw away this glyph's attempt and start it again. Ink already spent stays spent. */
    public void redoTarget() {
        if (!complete()) {
            samples.get(target).clear();
            message = "";
        }
    }

    private void evaluateTarget() {
        List<Vec2> points = samples.get(target);
        TraceResult result = evaluator.evaluate(ideals.get(target), points, tolerances.get(target));

        boolean ringOpen = isRing(target) && !RingGeometry.isClosed(points, tolerances.get(target));
        if (result.valid() && ringOpen) {
            message = "The ring has a gap in it — bring the ends together.";
            return;
        }
        if (!result.valid()) {
            message = result.coverage() < 0.85f
                    ? "Keep going — the glyph isn't finished."
                    : "Too far outside the lines. Try that one again.";
            return;
        }

        results.set(target, result);
        message = "";
        target++;
    }

    private boolean isRing(int index) {
        // A ring is the only glyph whose trace has to close, and it's the only
        // one placed at the centre at full scale, so ask the geometry directly.
        return idealLengths.get(index) > 2f;
    }

    private static float totalLength(List<StrokePath> strokes) {
        float total = 0f;
        for (StrokePath stroke : strokes) {
            List<Vec2> points = stroke.points();
            for (int i = 1; i < points.size(); i++) {
                total += points.get(i - 1).distanceTo(points.get(i));
            }
        }
        return total;
    }
}