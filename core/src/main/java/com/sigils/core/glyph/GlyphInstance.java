package com.sigils.core.glyph;

import com.sigils.core.geometry.Vec2;

import java.util.Objects;

/** A placed glyph: which glyph, and its transform on the canvas. Pure, serializable. */
public record GlyphInstance(String glyphId, Vec2 position, float rotation, float scale) {
    public GlyphInstance {
        Objects.requireNonNull(glyphId, "glyphId");
        Objects.requireNonNull(position, "position");
        if (scale <= 0f) {
            throw new IllegalArgumentException("scale must be > 0");
        }
    }
}