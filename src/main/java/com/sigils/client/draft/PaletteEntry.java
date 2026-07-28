package com.sigils.client.draft;

import com.sigils.core.draft.GlyphAvailability;
import com.sigils.core.glyph.Glyph;

/** A glyph as the palette sees it: the geometry, plus whether it's usable. */
public record PaletteEntry(Glyph glyph, GlyphAvailability availability) {

    public boolean locked() {
        return !availability.allowed();
    }
}
