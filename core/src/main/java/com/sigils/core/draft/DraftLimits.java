package com.sigils.core.draft;

/**
 * What a given canvas will accept. The drafting table is generous; the pocket
 * notepad is not.
 *
 * <p>Phase 5 will build these from the pen the player is holding. Until then the
 * two presets stand in — but everything already reads its limits from here, so
 * that change touches no other file.
 *
 * @param maxGlyphs           total placements allowed on the canvas
 * @param maxCrests           elemental cores allowed (1 until a pen permits compound spells)
 * @param maxComplexity       the most intricate glyph this pen can draw
 * @param canvasRadius        how far from the centre a glyph may sit, in canvas units
 * @param allowMultipleRings  intersecting rings are a later-tier feature
 */
public record DraftLimits(
        int maxGlyphs,
        int maxCrests,
        int maxComplexity,
        float canvasRadius,
        boolean allowMultipleRings
) {
    /** Absolute ceiling on placements in a serverbound draft, whatever the canvas claims. */
    public static final int HARD_MAX_GLYPHS = 32;

    /** Absolute ceiling on strokes in a serverbound draft, whatever the canvas claims. */
    public static final int HARD_MAX_STROKES = 64;

    public static final DraftLimits DRAFTING_TABLE = new DraftLimits(12, 1, 5, 0.45f, false);

    public static final DraftLimits NOTEPAD = new DraftLimits(6, 1, 2, 0.30f, false);
}