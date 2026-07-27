package com.sigils.core.glyph;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a glyph's local strokes into canvas-space strokes for a given placement.
 *
 * <p>A {@link Glyph} authors its strokes in its own 0..1 box, centred on
 * (0.5, 0.5). A {@link GlyphInstance} says where that box sits on the canvas,
 * how far it was turned, and how big it was drawn. This class is the only place
 * those two are combined — the canvas renderer and the server-side trace scorer
 * both call it, so a glyph is rendered exactly where it is scored.
 *
 * <p>Canvas space is 0..1 on both axes with <b>y growing downward</b>, matching
 * screen coordinates. Rotation is in <b>radians</b>, positive turning clockwise
 * on screen (a consequence of the downward y).
 */
public final class GlyphTransform {

    /** The middle of the canvas — the centre of the ring. */
    public static final Vec2 CANVAS_CENTRE = new Vec2(0.5f, 0.5f);

    /** The middle of a glyph's own authoring box. */
    private static final Vec2 LOCAL_CENTRE = new Vec2(0.5f, 0.5f);

    private GlyphTransform() {}

    /** Every stroke of {@code glyph}, transformed onto the canvas by {@code placement}. */
    public static List<StrokePath> toCanvas(Glyph glyph, GlyphInstance placement) {
        List<StrokePath> out = new ArrayList<>(glyph.strokes().size());
        for (StrokePath stroke : glyph.strokes()) {
            List<Vec2> points = new ArrayList<>(stroke.points().size());
            for (Vec2 local : stroke.points()) {
                points.add(point(local, placement));
            }
            out.add(new StrokePath(points));
        }
        return List.copyOf(out);
    }

    /** One local point, transformed onto the canvas. */
    public static Vec2 point(Vec2 local, GlyphInstance placement) {
        Vec2 centred = local.minus(LOCAL_CENTRE);
        float cos = (float) Math.cos(placement.rotation());
        float sin = (float) Math.sin(placement.rotation());
        Vec2 rotated = new Vec2(
                centred.x() * cos - centred.y() * sin,
                centred.x() * sin + centred.y() * cos);
        return placement.position().plus(rotated.scaled(placement.scale()));
    }

    /**
     * The tolerance band to score this placement against.
     *
     * <p>It scales with the placement: draw a glyph small and its lines are
     * closer together, so the band must tighten with them or a tiny glyph would
     * be trivially easy. This is why "complex spells demand cleaner drawing"
     * falls out of the data rather than out of a special case.
     */
    public static float toleranceFor(Glyph glyph, GlyphInstance placement) {
        return glyph.toleranceBand() * placement.scale();
    }
}