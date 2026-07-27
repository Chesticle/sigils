package com.sigils.core.draft;

import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;

import java.util.List;
import java.util.Optional;

/** What a draft costs in ink: each glyph's authored cost, scaled by how big it was drawn. */
public final class InkCost {

    private InkCost() {}

    /** Cost of one placement. Unknown glyph ids cost nothing — they're rejected elsewhere. */
    public static float of(GlyphInstance placement, GlyphLookup glyphs) {
        Optional<Glyph> glyph = glyphs.get(placement.glyphId());
        return glyph.map(g -> g.inkCost() * placement.scale()).orElse(0f);
    }

    /** Total cost of a whole draft. */
    public static float of(List<GlyphInstance> placements, GlyphLookup glyphs) {
        float total = 0f;
        for (GlyphInstance placement : placements) {
            total += of(placement, glyphs);
        }
        return total;
    }
}