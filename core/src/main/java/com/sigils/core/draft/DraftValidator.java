package com.sigils.core.draft;

import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphTransform;
import com.sigils.core.spell.SpellGraph;
import com.sigils.core.spell.SpellGraphBuilder;
import com.sigils.core.spell.SpellGraphValidator;
import com.sigils.core.spell.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks a draft against both the structural rules (Phase 1B) and the limits of
 * the canvas it was drawn on.
 *
 * <p>Errors are written to be shown to a player verbatim: name the glyph, name
 * the number, say what to do. "Invalid spell" teaches nobody anything.
 */
public final class DraftValidator {

    private DraftValidator() {}

    public static ValidationResult validate(
            List<GlyphInstance> placements, GlyphLookup glyphs, DraftLimits limits) {

        List<String> errors = new ArrayList<>();

        if (placements.size() > limits.maxGlyphs()) {
            errors.add("Too many glyphs: " + placements.size()
                    + " placed, this canvas holds " + limits.maxGlyphs() + ".");
        }

        for (GlyphInstance placement : placements) {
            Optional<Glyph> maybe = glyphs.get(placement.glyphId());
            if (maybe.isEmpty()) {
                errors.add("Unknown glyph: " + placement.glyphId() + ".");
                continue;
            }
            Glyph glyph = maybe.get();

            if (glyph.complexity() > limits.maxComplexity()) {
                errors.add("'" + placement.glyphId() + "' is too intricate for this pen"
                        + " (complexity " + glyph.complexity()
                        + ", limit " + limits.maxComplexity() + ").");
            }
            float fromCentre = placement.position().distanceTo(GlyphTransform.CANVAS_CENTRE);
            if (fromCentre > limits.canvasRadius() + 1e-4f) {
                errors.add("'" + placement.glyphId() + "' sits outside the circle.");
            }
        }

        // Structural rules: a crest, a ring, and every modifier attached.
        SpellGraph graph = SpellGraphBuilder.build(placements, glyphs);
        errors.addAll(SpellGraphValidator.validate(graph).errors());

        if (graph.crests().size() > limits.maxCrests()) {
            errors.add("This pen can bind " + limits.maxCrests() + " crest(s) in one circle; "
                    + graph.crests().size() + " are placed.");
        }
        if (!limits.allowMultipleRings() && graph.rings().size() > 1) {
            errors.add("Intersecting rings need a finer pen — draw one ring.");
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failed(errors);
    }

    /** Convenience for the UI: is the centre of this placement inside the canvas? */
    public static boolean withinCanvas(Vec2 position, DraftLimits limits) {
        return position.distanceTo(GlyphTransform.CANVAS_CENTRE) <= limits.canvasRadius();
    }
}