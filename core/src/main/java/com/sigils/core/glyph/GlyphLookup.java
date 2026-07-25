package com.sigils.core.glyph;

import java.util.Optional;

/** Resolves a glyph id to its definition. Backed by a map in tests, a registry in-game. */
@FunctionalInterface
public interface GlyphLookup {
    Optional<Glyph> get(String glyphId);
}