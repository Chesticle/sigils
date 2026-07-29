package com.sigils.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.*;

import com.sigils.core.glyph.Glyph;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;

import com.sigils.core.knowledge.KnownGlyphs;
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

    /**
     * The glyphs every player knows without being taught — the
     * {@code #sigils:innate} tag, read as a {@link KnownGlyphs}.
     *
     * <p>Safe on both sides: the glyph registry is network-synced (Phase 4A
     * registered it with a network codec precisely so the palette could draw
     * from it), and registry tags travel with it.
     */
    public static KnownGlyphs innate(RegistryAccess access) {
        Registry<GlyphDefinition> registry = access.lookupOrThrow(SigilsRegistries.GLYPH);
        Set<String> ids = new HashSet<>();
        for (Holder<GlyphDefinition> holder : registry.getTagOrEmpty(SigilsGlyphTags.INNATE)) {
            Identifier id = registry.getKey(holder.value());
            if (id != null) {
                ids.add(id.toString());
            }
        }
        return ids.isEmpty() ? KnownGlyphs.NONE : new KnownGlyphs(ids);
    }

    /**
     * Whether a glyph id resolves to something the datapacks actually loaded.
     *
     * <p>Walks the registry, which is more work than a lookup needs — but this
     * runs once per right-click rather than per frame, and doing it this way
     * means the check shares its definition of "exists" with {@link #loadAll}
     * instead of having a second, subtly different one.
     */
    public static boolean exists(RegistryAccess access, String glyphId) {
        return loadAll(access).containsKey(glyphId);
    }

    /**
     * Every glyph id in the registry, sorted.
     *
     * <p>Takes a {@link HolderLookup.Provider} rather than a
     * {@link RegistryAccess} because the creative tab is handed the weaker of
     * the two, and this is the only caller that can't offer the stronger one.
     */
    public static List<String> ids(HolderLookup.Provider registries) {
        return registries.lookup(SigilsRegistries.GLYPH)
                .map(lookup -> lookup.listElementIds()
                        .map(key -> key.identifier().toString())
                        .sorted()
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * Lang key for a glyph's display name: {@code sigils:mod_beam} becomes
     * {@code glyph.sigils.mod_beam}.
     *
     * <p>Third place this transform is needed — the palette tooltip has done it
     * inline since Phase 5C, and Section 4's unlock message needs the same key.
     * By this project's own rule that's the moment it stops being a line and
     * becomes a method. Section 6 points the tooltip at it.
     */
    public static String nameKey(String glyphId) {
        return "glyph." + glyphId.replace(':', '.');
    }

    /** A {@link GlyphLookup} over a snapshot of the registry. */
    public static GlyphLookup lookup(RegistryAccess access) {
        Map<String, Glyph> glyphs = loadAll(access);
        return id -> Optional.ofNullable(glyphs.get(id));
    }
}