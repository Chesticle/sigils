package com.sigils.client.draft;

import com.sigils.core.draft.GlyphAvailability;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.registry.SigilsGlyphs;

/**
 * A snapshot of the glyph registry, taken when the drafting screen opens.
 *
 * <p>The registry is synced to the client, so the palette needs no packet of its
 * own: a datapack-added glyph arrives with its strokes, its tolerance and its
 * cost, and shows up here without a line of UI code.
 */
public final class ClientGlyphs {

    private final Map<String, Glyph> glyphs;

    private ClientGlyphs(Map<String, Glyph> glyphs) {
        this.glyphs = glyphs;
    }

    /** Take the snapshot. Call on screen open, never per frame. */
    public static ClientGlyphs snapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return new ClientGlyphs(Map.of());
        }
        return new ClientGlyphs(SigilsGlyphs.loadAll(minecraft.level.registryAccess()));
    }

    public GlyphLookup lookup() {
        return id -> Optional.ofNullable(glyphs.get(id));
    }

    public Optional<Glyph> get(String id) {
        return Optional.ofNullable(glyphs.get(id));
    }

    /**
     * Everything in the registry, in palette order, each tagged with whether
     * this pen can draw it.
     *
     * <p>Nothing is filtered out. A glyph the player can't use is more useful
     * on screen than absent: it tells them the glyph exists and what it would
     * take to draw it, which is the difference between progression and a bug
     * report.
     */
    public List<PaletteEntry> palette(DraftLimits limits, boolean penPresent) {
        List<Glyph> sorted = new ArrayList<>(glyphs.values());
        sorted.sort(Comparator
                .comparingInt((Glyph g) -> g.role().ordinal())
                .thenComparing(Glyph::id));

        List<PaletteEntry> entries = new ArrayList<>(sorted.size());
        for (Glyph glyph : sorted) {
            entries.add(new PaletteEntry(glyph, GlyphAvailability.of(glyph, limits, penPresent)));
        }
        return List.copyOf(entries);
    }
}