package com.sigils.registry;

import net.minecraft.tags.TagKey;

import com.sigils.Sigils;

/** Tags over the {@code sigils:glyph} datapack registry. */
public final class SigilsGlyphTags {

    private SigilsGlyphTags() {}

    /**
     * Glyphs every player may draw without learning them.
     *
     * <p>Kept deliberately tiny — a ring, one crest, one target modifier. The
     * whole point of Phase 7 is that the rest are found.
     */
    public static final TagKey<GlyphDefinition> INNATE =
            TagKey.create(SigilsRegistries.GLYPH, Sigils.id("innate"));
}