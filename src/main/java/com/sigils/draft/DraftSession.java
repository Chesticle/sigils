package com.sigils.client.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.DraftValidator;
import com.sigils.core.draft.InkCost;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphTransform;
import com.sigils.core.spell.ValidationResult;

/**
 * The drawing in progress, client-side only.
 *
 * <p>Nothing here decides what is legal — {@link DraftValidator} does, and this
 * class just asks it again whenever something changes. Recomputing on mutation
 * rather than per frame matters: validation walks every placement and builds a
 * graph, which is cheap once per click and wasteful sixty times a second.
 */
public final class DraftSession {

    /** Scale a glyph gets when first taken from the palette. */
    public static final float DEFAULT_SCALE = 0.30f;
    private static final float MIN_SCALE = 0.10f;
    private static final float MAX_SCALE = 1.00f;

    /** Rotation step, in radians — a 15° detent, so turned glyphs stay tidy. */
    public static final float ROTATION_STEP = (float) (Math.PI / 12);

    /** Snap distance, in canvas units, for the centre and the ring. */
    private static final float SNAP = 0.035f;

    private final GlyphLookup glyphs;
    private final List<GlyphInstance> placements = new ArrayList<>();

    private DraftLimits limits;
    private GlyphInstance held;
    private ValidationResult validation;
    private float inkCost;

    public DraftSession(GlyphLookup glyphs, DraftLimits limits) {
        this.glyphs = glyphs;
        this.limits = limits;
        revalidate();
    }

    // ------------------------------------------------------------------ state

    public List<GlyphInstance> placements() {
        return List.copyOf(placements);
    }

    public Optional<GlyphInstance> held() {
        return Optional.ofNullable(held);
    }

    public ValidationResult validation() {
        return validation;
    }

    public float inkCost() {
        return inkCost;
    }

    public DraftLimits limits() {
        return limits;
    }

    /** The player swapped the pen mid-session; the rules change under them. */
    public void limits(DraftLimits updated) {
        if (!updated.equals(limits)) {
            limits = updated;
            revalidate();
        }
    }

    // --------------------------------------------------------------- mutation

    /** Take a fresh glyph from the palette. Anything already held is dropped. */
    public void take(Glyph glyph) {
        held = new GlyphInstance(glyph.id(), GlyphTransform.CANVAS_CENTRE, 0f, DEFAULT_SCALE);
    }

    /** Put the held glyph down at a canvas point, snapped. */
    public void place(Vec2 point) {
        if (held == null) {
            return;
        }
        Vec2 snapped = snap(point);
        placements.add(new GlyphInstance(held.glyphId(), snapped, held.rotation(), held.scale()));
        held = null;
        revalidate();
    }

    /** Pick a placed glyph back up, keeping its size and rotation. */
    public void lift(int index) {
        if (index < 0 || index >= placements.size()) {
            return;
        }
        held = placements.remove(index);
        revalidate();
    }

    public void scaleHeld(float factor) {
        if (held != null) {
            float scale = Math.clamp(held.scale() * factor, MIN_SCALE, MAX_SCALE);
            held = new GlyphInstance(held.glyphId(), held.position(), held.rotation(), scale);
        }
    }

    public void rotateHeld(float radians) {
        if (held != null) {
            held = new GlyphInstance(held.glyphId(), held.position(),
                    held.rotation() + radians, held.scale());
        }
    }

    public void discardHeld() {
        held = null;
    }

    public void clear() {
        placements.clear();
        held = null;
        revalidate();
    }

    // ---------------------------------------------------------------- queries

    /** Topmost placement under a canvas point, or -1. Later placements win. */
    public int indexAt(Vec2 point) {
        for (int i = placements.size() - 1; i >= 0; i--) {
            GlyphInstance placement = placements.get(i);
            if (placement.position().distanceTo(point) <= 0.5f * placement.scale()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Snapping guides: the centre of the circle and the ring itself are the two
     * places a glyph usually wants to be, so make them easy to hit. Everything
     * else is free placement, clamped inside the canvas.
     */
    private Vec2 snap(Vec2 point) {
        Vec2 centre = GlyphTransform.CANVAS_CENTRE;
        float distance = point.distanceTo(centre);

        if (distance <= SNAP) {
            return centre;
        }
        float radius = limits.canvasRadius();
        if (Math.abs(distance - radius) <= SNAP) {
            return onCircle(centre, point, radius);
        }
        if (distance > radius) {
            return onCircle(centre, point, radius); // clamp inside the circle
        }
        return point;
    }

    private static Vec2 onCircle(Vec2 centre, Vec2 towards, float radius) {
        Vec2 offset = towards.minus(centre);
        float length = offset.length();
        if (length < 1e-4f) {
            return new Vec2(centre.x() + radius, centre.y());
        }
        return centre.plus(offset.scaled(radius / length));
    }

    private void revalidate() {
        validation = DraftValidator.validate(placements, glyphs, limits);
        inkCost = InkCost.of(placements, glyphs);
    }
}