package com.sigils.core.spell;

import com.sigils.core.glyph.GlyphInstance;

import java.util.List;
import java.util.Objects;

/**
 * Placements organised by role, plus the modifier→crest attachments.
 *
 * <p>Supports many crests so compound spells are representable without a data
 * migration later; single-crest spells simply have one entry in {@link #crests}.
 */
public record SpellGraph(
        List<GlyphInstance> crests,
        List<GlyphInstance> modifiers,
        List<GlyphInstance> rings,
        List<Edge> edges
) {
    public SpellGraph {
        crests = List.copyOf(Objects.requireNonNull(crests, "crests"));
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        rings = List.copyOf(Objects.requireNonNull(rings, "rings"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
    }

    /** Attaches a modifier (index into {@link #modifiers}) to a crest (index into {@link #crests}). */
    public record Edge(int modifierIndex, int crestIndex) {}
}