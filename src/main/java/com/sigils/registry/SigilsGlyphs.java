package com.sigils.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphLookup;

/** Loads all datapack glyphs into pure-core {@link Glyph}s, and hands out a lookup. */
public final class SigilsGlyphs {

    private SigilsGlyphs() {}

    public static Map<String, Glyph> loadAll(RegistryAccess access) {
        Registry<GlyphDefinition> registry = access.lookupOrThrow(SigilsRegistries.GLYPH);
        Map<String, Glyph> glyphs = new HashMap<>();
        for (Identifier id : registry.keySet()) {
            GlyphDefinition definition = registry.getValue(id);
            if (definition != null) {
                glyphs.put(id.toString(), definition.toCore(id));
            }
        }
        return Map.copyOf(glyphs);
    }

    /** A {@link GlyphLookup} over a snapshot of the registry. */
    public static GlyphLookup lookup(RegistryAccess access) {
        Map<String, Glyph> glyphs = loadAll(access);
        return id -> Optional.ofNullable(glyphs.get(id));
    }
}