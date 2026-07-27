package com.sigils.client.draft;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphTransform;


/**
 * Draws normalised 0..1 geometry into a square box on screen.
 *
 * <p>Used twice with two different boxes: the canvas (for placed glyphs, whose
 * strokes have been transformed into canvas space) and a palette cell (for a
 * glyph's own local strokes). One renderer, because in both cases the input is
 * "points in a unit square".
 */
public final class CanvasRenderer {

    private CanvasRenderer() {}

    /** A single line segment of the given thickness, at any angle. */
    public static void segment(GuiGraphicsExtractor graphics,
                               float x1, float y1, float x2, float y2,
                               float thickness, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x1, y1);
        graphics.pose().rotate((float) Math.atan2(dy, dx));
        graphics.pose().translate(0f, -thickness / 2f);
        graphics.pose().scale(length, thickness);
        // A 1x1 rectangle, stretched by the pose into the segment.
        graphics.fill(0, 0, 1, 1, argb);
        graphics.pose().popMatrix();
    }

    /** Strokes in unit space, drawn into the box at (boxX, boxY) of side boxSize. */
    public static void strokes(GuiGraphicsExtractor graphics, List<StrokePath> strokes,
                               float boxX, float boxY, float boxSize,
                               float thickness, int argb) {
        for (StrokePath stroke : strokes) {
            List<Vec2> points = stroke.points();
            for (int i = 1; i < points.size(); i++) {
                Vec2 a = points.get(i - 1);
                Vec2 b = points.get(i);
                segment(graphics,
                        boxX + a.x() * boxSize, boxY + a.y() * boxSize,
                        boxX + b.x() * boxSize, boxY + b.y() * boxSize,
                        thickness, argb);
            }
        }
    }

    /** A placed glyph, transformed onto the canvas by its placement. */
    public static void placed(GuiGraphicsExtractor graphics, Glyph glyph, GlyphInstance placement,
                              float boxX, float boxY, float boxSize, float thickness, int argb) {
        strokes(graphics, GlyphTransform.toCanvas(glyph, placement),
                boxX, boxY, boxSize, thickness, argb);
    }

    /** A closed circle, for the ring guide and the snap radius. */
    public static void circle(GuiGraphicsExtractor graphics,
                              float centreX, float centreY, float radius,
                              int segments, float thickness, int argb) {
        float previousX = centreX + radius;
        float previousY = centreY;
        for (int i = 1; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = centreX + radius * (float) Math.cos(angle);
            float y = centreY + radius * (float) Math.sin(angle);
            segment(graphics, previousX, previousY, x, y, thickness, argb);
            previousX = x;
            previousY = y;
        }
    }
}