package com.sigils.core.sigil;

/**
 * Why a spell press is or isn't going to print.
 *
 * <p>A reason, not a sentence — the same split Phase 5C used for locked palette
 * entries. {@code core} decides <em>which</em> problem it is; the block decides
 * what language to say it in.
 */
public enum PressReadiness {

    /** Everything is loaded and the surface is free. */
    READY,

    /** No inscribed parchment in the template slot. */
    NO_TEMPLATE,

    /** No pen. */
    NO_PEN,

    /** No ink, or not enough of it to draw with. */
    NO_INK,

    /** Something is already in the cell the press draws into. */
    OBSTRUCTED,

    /** Too many sigils nearby already. See {@link PressRules#NEARBY_LIMIT}. */
    CROWDED;

    public boolean ready() {
        return this == READY;
    }
}