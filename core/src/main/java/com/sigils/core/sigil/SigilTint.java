package com.sigils.core.sigil;

/**
 * What colour a drawn sigil renders in.
 *
 * <p>Ink tints were chosen in Phase 5 for an ink bar on a dark GUI panel, so they
 * are all very dark — magical ink is {@code #2A2440} and netherite ink is
 * {@code #12100F}. A block tint is <em>multiplicative</em>: that colour applied to
 * a texture is black on black. So the hue is lifted toward white far enough to
 * read on stone, and then dimmed by wear.
 *
 * <p>Pure integer arithmetic, so it's tested rather than eyeballed. The failure
 * this exists to prevent — a sigil that placed correctly and is invisible — looks
 * exactly like a sigil that didn't place.
 */
public final class SigilTint {

    /** How far each channel is pulled toward white. 0 keeps the ink; 1 is white. */
    public static final float LIFT = 0.45f;

    /** What's left of the colour at zero integrity. Never black — a dead sigil is still visible. */
    public static final float MIN_BRIGHTNESS = 0.35f;

    /** Plain white, for when there's no block entity to ask (item frames, model previews). */
    public static final int FALLBACK = 0xFFFFFFFF;

    private SigilTint() {}

    /**
     * @param inkTint   the grade's RGB, no alpha, straight off {@code InkGrade.tint()}
     * @param integrity 0..1
     * @return ARGB with the alpha byte set, which block tints tolerate and GUI
     *         colours have required since 1.21.6
     */
    public static int decal(int inkTint, float integrity) {
        float dim = MIN_BRIGHTNESS + (1f - MIN_BRIGHTNESS) * Math.clamp(integrity, 0f, 1f);
        return 0xFF000000
                | (channel((inkTint >> 16) & 0xFF, dim) << 16)
                | (channel((inkTint >> 8) & 0xFF, dim) << 8)
                | channel(inkTint & 0xFF, dim);
    }

    private static int channel(int value, float dim) {
        float lifted = value + (255f - value) * LIFT;
        return Math.clamp(Math.round(lifted * dim), 0, 255);
    }
}