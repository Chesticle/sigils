package com.sigils.client.draft;

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
     * The glyphs this player may place right now: ones they know, that this pen
     * can draw. Sorted by role so the palette reads crest → modifier → ring.
     */
    public List<Glyph> palette(DraftLimits limits) {
        List<Glyph> visible = new ArrayList<>();
        for (Glyph glyph : glyphs.values()) {
            if (glyph.complexity() > limits.maxComplexity()) {
                continue; // the pen can't draw it — Phase 5 makes this per-tier
            }
            if (!isKnown(glyph)) {
                continue;
            }
            visible.add(glyph);
        }
        visible.sort(Comparator
                .comparingInt((Glyph g) -> g.role().ordinal())
                .thenComparing(Glyph::id));
        return List.copyOf(visible);
    }

    /**
     * Knowledge gating lands in Phase 6 (research and unlocks). Until then every
     * glyph is known — but the seam exists, so that phase changes this method
     * and nothing else.
     */
    private static boolean isKnown(Glyph glyph) {
        return true;
    }
}