package com.sigils.core.draft;

import com.sigils.core.glyph.Glyph;

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

    public boolean allowed() {
        return reason == Reason.AVAILABLE;
    }

    /** What this canvas, with this pen, makes of this glyph. */
    public static GlyphAvailability of(Glyph glyph, DraftLimits limits) {
        if (glyph.complexity() > limits.maxComplexity()) {
            return new GlyphAvailability(
                    Reason.TOO_COMPLEX, glyph.complexity(), limits.maxComplexity());
        }
        return OK;
    }

    /** The same question, when there may be no pen in the table at all. */
    public static GlyphAvailability of(Glyph glyph, DraftLimits limits, boolean penPresent) {
        return penPresent ? of(glyph, limits) : NO_PEN;
    }
}