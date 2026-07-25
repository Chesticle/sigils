package com.sigils.core.spell;

import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Turns a flat list of placements into a role-separated, edge-linked {@link SpellGraph}. */
public final class SpellGraphBuilder {

    private SpellGraphBuilder() {}

    /**
     * Organises placements by role and attaches each modifier to its nearest
     * crest. Unknown glyph ids are skipped (a placement referencing a glyph the
     * server doesn't have simply doesn't participate). LINK glyphs are deferred
     * to a later phase.
     */
    public static SpellGraph build(List<GlyphInstance> placements, GlyphLookup glyphs) {
        List<GlyphInstance> crests = new ArrayList<>();
        List<GlyphInstance> modifiers = new ArrayList<>();
        List<GlyphInstance> rings = new ArrayList<>();

        for (GlyphInstance instance : placements) {
            Optional<Glyph> maybe = glyphs.get(instance.glyphId());
            if (maybe.isEmpty()) {
                continue;
            }
            switch (maybe.get().role()) {
                case CREST -> crests.add(instance);
                case MODIFIER -> modifiers.add(instance);
                case RING -> rings.add(instance);
                case LINK -> { /* compound-spell linking arrives in a later phase */ }
            }
        }

        List<SpellGraph.Edge> edges = new ArrayList<>();
        for (int m = 0; m < modifiers.size(); m++) {
            int nearest = nearestCrestIndex(modifiers.get(m), crests);
            if (nearest >= 0) {
                edges.add(new SpellGraph.Edge(m, nearest));
            }
        }

        return new SpellGraph(crests, modifiers, rings, edges);
    }

    private static int nearestCrestIndex(GlyphInstance modifier, List<GlyphInstance> crests) {
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int c = 0; c < crests.size(); c++) {
            float d = modifier.position().distanceSquaredTo(crests.get(c).position());
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }
}