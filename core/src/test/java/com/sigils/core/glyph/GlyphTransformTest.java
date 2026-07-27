package com.sigils.core.glyph;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GlyphTransformTest {

    /** A horizontal line across the middle of the glyph's own box. */
    private static final Glyph BAR = new Glyph(
            "test:bar",
            GlyphRole.RING,
            List.of(StrokePath.of(new Vec2(0f, 0.5f), new Vec2(1f, 0.5f))),
            0.05f, 1, 1f,
            Optional.empty(), Optional.empty());

    @Test
    @DisplayName("placed dead centre at scale 1, the geometry is unchanged")
    void identityPlacement() {
        GlyphInstance placed = new GlyphInstance("test:bar", new Vec2(0.5f, 0.5f), 0f, 1f);
        StrokePath stroke = GlyphTransform.toCanvas(BAR, placed).get(0);

        assertEquals(0f, stroke.points().get(0).x(), 1e-5);
        assertEquals(0.5f, stroke.points().get(0).y(), 1e-5);
        assertEquals(1f, stroke.points().get(1).x(), 1e-5);
    }

    @Test
    @DisplayName("scale shrinks the glyph around its own centre, not around the origin")
    void scaleShrinksAroundGlyphCentre() {
        GlyphInstance placed = new GlyphInstance("test:bar", new Vec2(0.5f, 0.5f), 0f, 0.5f);
        StrokePath stroke = GlyphTransform.toCanvas(BAR, placed).get(0);

        // The bar spanned 0..1; at half scale it spans 0.25..0.75, still centred.
        assertEquals(0.25f, stroke.points().get(0).x(), 1e-5);
        assertEquals(0.75f, stroke.points().get(1).x(), 1e-5);
        assertEquals(0.5f, stroke.points().get(0).y(), 1e-5);
    }

    @Test
    @DisplayName("a quarter turn moves the right-hand end below the centre")
    void rotationTurnsTheGlyph() {
        GlyphInstance placed = new GlyphInstance(
                "test:bar", new Vec2(0.5f, 0.5f), (float) (Math.PI / 2), 1f);
        StrokePath stroke = GlyphTransform.toCanvas(BAR, placed).get(0);

        Vec2 rightEnd = stroke.points().get(1); // was (1.0, 0.5), i.e. right of centre
        assertEquals(0.5f, rightEnd.x(), 1e-5, "should now sit on the vertical axis");
        assertEquals(1.0f, rightEnd.y(), 1e-5, "y grows downward, so it lands below centre");
    }

    @Test
    @DisplayName("the tolerance band scales with the placement")
    void toleranceScalesWithPlacement() {
        GlyphInstance small = new GlyphInstance("test:bar", new Vec2(0.5f, 0.5f), 0f, 0.5f);
        assertEquals(0.025f, GlyphTransform.toleranceFor(BAR, small), 1e-6);
    }
}