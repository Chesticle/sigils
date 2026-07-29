package com.sigils.core.draft;

import com.sigils.core.glyph.Glyph;
import com.sigils.core.knowledge.KnownGlyphs;

/**
 * Whether a glyph may be placed right now, and if not, why — with the numbers
 * needed to say so precisely.
 *
 * <p>Reasons travel as data rather than as a sentence so the screen can build a
 * translatable {@code Component} from them. That's the difference between a
 * message this mod can ship in one language and one it can ship in twenty.
 *
 * @param reason    why, or {@link Reason#AVAILABLE}
 * @param required  what the glyph demands (its complexity, for now)
 * @param available what the current tools offer
 */
public record GlyphAvailability(Reason reason, int required, int available) {

    public enum Reason {
        /** Place away. */
        AVAILABLE,
        /** There's no pen in the table at all. */
        NO_PEN,
        /** The pen cannot draw anything this intricate. */
        TOO_COMPLEX,
        /** The player hasn't learned it yet — Phase 7's knowledge system. */
        NOT_LEARNED
    }

    public static final GlyphAvailability OK = new GlyphAvailability(Reason.AVAILABLE, 0, 0);
    public static final GlyphAvailability NO_PEN = new GlyphAvailability(Reason.NO_PEN, 0, 0);
    public static final GlyphAvailability NOT_LEARNED =
            new GlyphAvailability(Reason.NOT_LEARNED, 0, 0);

    public boolean allowed() {
        return reason == Reason.AVAILABLE;
    }

    /**
     * What this canvas, with this pen and this player's knowledge, makes of
     * this glyph.
     *
     * <p>The order of the three checks is a design decision, not an accident.
     * With no pen nothing at all is drawable, so that wins. After that,
     * <em>not learned</em> beats <em>too complex</em>: telling a player that a
     * glyph they have never heard of would need a diamond pen leaks the shape of
     * the progression they haven't earned yet, and answers a question they
     * didn't ask. Learn it first; then find out your quill can't manage it.
     *
     * @param known the glyphs this player may draw — learned plus innate,
     *              already folded together by the caller
     */
    public static GlyphAvailability of(Glyph glyph, DraftLimits limits,
                                       boolean penPresent, KnownGlyphs known) {
        if (!penPresent) {
            return NO_PEN;
        }
        if (!known.knows(glyph.id())) {
            return NOT_LEARNED;
        }
        if (glyph.complexity() > limits.maxComplexity()) {
            return new GlyphAvailability(
                    Reason.TOO_COMPLEX, glyph.complexity(), limits.maxComplexity());
        }
        return OK;
    }
}