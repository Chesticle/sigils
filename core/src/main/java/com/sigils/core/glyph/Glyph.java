package com.sigils.core.glyph;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A drawable symbol: its strokes, how tightly it must be traced, its cost, and
 * its effect on a spell.
 *
 * <p>A crest carries an {@code ElementalMixture} contribution; a modifier
 * carries a {@link ModifierOp}. The constructor enforces that pairing so a
 * malformed glyph can't exist.
 *
 * @param toleranceBand how far a trace may stray (0..1 canvas units). Tighter
 *                      bands make complex glyphs harder — the "complex spells
 *                      demand cleaner drawing" rule lives here.
 * @param complexity    an author-facing difficulty tag (used by later phases)
 * @param inkCost       ink consumed to place this glyph
 */
public record Glyph(
        String id,
        GlyphRole role,
        List<StrokePath> strokes,
        float toleranceBand,
        int complexity,
        float inkCost,
        Optional<ElementalMixture> contribution,
        Optional<ModifierOp> operation
) {
    public Glyph {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(strokes, "strokes");
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(operation, "operation");
        strokes = List.copyOf(strokes);
        if (toleranceBand <= 0f) {
            throw new IllegalArgumentException("toleranceBand must be > 0 for glyph " + id);
        }
        if (role == GlyphRole.CREST && contribution.isEmpty()) {
            throw new IllegalArgumentException("Crest '" + id + "' must declare an elemental contribution");
        }
        if (role == GlyphRole.MODIFIER && operation.isEmpty()) {
            throw new IllegalArgumentException("Modifier '" + id + "' must declare an operation");
        }
    }
}