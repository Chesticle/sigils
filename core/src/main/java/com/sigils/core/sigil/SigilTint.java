package com.sigils.core.sigil;

/**
 * What colour a drawn sigil renders in.
 *
 * <p>Ink tints were chosen in Phase 5 for an ink bar on a dark GUI panel, so they
 * are all very dark — magical ink is {@code #2A2440} and netherite ink is
 * {@code #12100F}. A block tint is <em>multiplicative</em>, so that colour applied
 * to a texture is black on black.
 *
 * <p>The whole job is therefore "make this bright enough to see without making it
 * a different colour", and the operation that does that is a <b>scale</b>: multiply
 * every channel by one factor and the ratios between them — which is what hue
 * is — survive untouched. Lifting each channel toward white instead would raise
 * the brightness and flatten the hue at the same time, and #2A2440 lifted 45% of
 * the way to white is (138, 135, 150), which is grey.
 *
 * <p>Pure integer arithmetic, so it's tested rather than eyeballed.
 */
public final class SigilTint {

    /** Darkest an ink is ever allowed to render at full integrity. */
    public static final int MIN_PEAK = 96;

    /** Brightest. A pale ink lands near here, a near-black one near MIN_PEAK. */
    public static final int MAX_PEAK = 235;

    /** What's left at zero integrity. Never 0 — a dead sigil is still visible. */
    public static final float WEAR_FLOOR = 0.4f;

    /** Extra brightness while the sigil is firing. Clamped per channel on the way out. */
    public static final float ACTIVE_BOOST = 1.45f;

    /** Plain white, for when there's no block entity to ask. */
    public static final int FALLBACK = 0xFFFFFFFF;

    private SigilTint() {}

    /**
     * @param inkTint   the grade's RGB, no alpha, straight off {@code InkGrade.tint()}
     * @param integrity 0..1
     * @return ARGB with the alpha byte set
     */
    public static int decal(int inkTint, float integrity) {
        return decal(inkTint, integrity, false);
    }

    /**
     * @param active true for the moment the sigil is discharging. Multiplies
     *               brightness only — the channel ratios, and therefore the ink's
     *               hue, are untouched, so a firing sigil is the same colour
     *               turned up rather than a different colour.
     */
    public static int decal(int inkTint, float integrity, boolean active) {
        int r = (inkTint >> 16) & 0xFF;
        int g = (inkTint >> 8) & 0xFF;
        int b = inkTint & 0xFF;

        float wear = WEAR_FLOOR + (1f - WEAR_FLOOR) * Math.clamp(integrity, 0f, 1f);
        if (active) {
            wear *= ACTIVE_BOOST;
        }
        int peak = Math.max(r, Math.max(g, b));

        if (peak == 0) {
            // Pure black has no hue to preserve. Render it neutral rather than
            // dividing by zero to find out what colour nothing is.
            int grey = Math.clamp(Math.round(MIN_PEAK * wear), 0, 255);
            return 0xFF000000 | (grey << 16) | (grey << 8) | grey;
        }

        // sqrt, not linear: a near-black ink has to come up a long way to be seen
        // at all, and a linear curve would put every dark ink at the same
        // brightness. The curve keeps netherite visibly darker than magical.
        float target = MIN_PEAK + (MAX_PEAK - MIN_PEAK) * (float) Math.sqrt(peak / 255f);
        float scale = (target * wear) / peak;

        return 0xFF000000
                | (channel(r, scale) << 16)
                | (channel(g, scale) << 8)
                | channel(b, scale);
    }

    private static int channel(int value, float scale) {
        return Math.clamp(Math.round(value * scale), 0, 255);
    }
}