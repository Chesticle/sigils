package com.sigils.core.knowledge;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The glyphs one player may draw.
 *
 * <p>Immutable, and every transition returns a new instance — this ends up in a
 * data attachment that is read on the render thread and written on the server
 * thread, and shared mutable state there is how a palette ends up disagreeing
 * with the inscriber.
 *
 * <p>Glyph ids are plain strings rather than a Minecraft type on purpose:
 * {@link com.sigils.core.glyph.GlyphInstance} already stores a {@code String
 * glyphId}, so the set and the placements it gates speak the same language and
 * no conversion happens anywhere in the hot path.
 *
 * @param ids fully-qualified glyph ids, e.g. {@code "sigils:mod_beam"}
 */
public record KnownGlyphs(Set<String> ids) {

    /** A player who has learned nothing. Also the attachment's default value. */
    public static final KnownGlyphs NONE = new KnownGlyphs(Set.of());

    public KnownGlyphs {
        Objects.requireNonNull(ids, "ids");
        // Defensive copy: the caller may still hold the set it handed us, and
        // this record is going to be shared across threads.
        ids = Set.copyOf(ids);
    }

    public boolean knows(String glyphId) {
        return ids.contains(glyphId);
    }

    public int size() {
        return ids.size();
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /**
     * Learn one glyph.
     *
     * <p>Returns {@code this} — the same object, not an equal one — when the
     * glyph was already known. Callers use that identity check to decide whether
     * anything is worth saving, syncing or announcing, so it is a contract and
     * not an optimisation.
     */
    public KnownGlyphs learned(String glyphId) {
        Objects.requireNonNull(glyphId, "glyphId");
        if (ids.contains(glyphId)) {
            return this;
        }
        Set<String> grown = new LinkedHashSet<>(ids);
        grown.add(glyphId);
        return new KnownGlyphs(grown);
    }

    /** Unlearn one glyph. Same identity contract as {@link #learned(String)}. */
    public KnownGlyphs forgotten(String glyphId) {
        if (!ids.contains(glyphId)) {
            return this;
        }
        Set<String> shrunk = new LinkedHashSet<>(ids);
        shrunk.remove(glyphId);
        return new KnownGlyphs(shrunk);
    }

    /**
     * Everything in either set.
     *
     * <p>The one caller that matters folds the innate glyphs into what a player
     * has actually learned, so nothing downstream has to know that "known" has
     * two sources.
     */
    public KnownGlyphs and(KnownGlyphs other) {
        if (other.ids.isEmpty()) {
            return this;
        }
        if (ids.isEmpty()) {
            return other;
        }
        Set<String> merged = new LinkedHashSet<>(ids);
        return merged.addAll(other.ids) ? new KnownGlyphs(merged) : this;
    }

    /**
     * Stable, alphabetical order.
     *
     * <p>{@link Set#copyOf} makes no promise about iteration order, so anything
     * that leaves this class — the codec, the packet, the command's output —
     * goes through here. A save file whose bytes change when nothing changed is
     * a diff nobody can read and a chunk-save nobody needed.
     */
    public List<String> sorted() {
        return ids.stream().sorted().toList();
    }
}