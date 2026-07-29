package com.sigils.core.sigil;

import java.util.ArrayList;
import java.util.List;

/**
 * The cells a multiblock sigil occupies.
 *
 * <p>A ring one cell thick, because that is what a magic circle looks like and
 * because a filled disc at radius 16 is eight hundred block entities.
 */
public final class SigilFootprint {

    /**
     * The largest ring this implementation will draw, whatever a pen claims.
     *
     * <p>An honest engineering limit rather than a balance number. One block
     * entity per cell is a fine representation up to a few hundred and a bad one
     * past that, and the forbidden pen's 32 would be roughly two hundred cells.
     * Phase 9 can raise this by changing how a structure is stored; until then,
     * clamping is better than shipping a pen that lags the server.
     */
    public static final int MAX_RADIUS = 16;

    private SigilFootprint() {}

    /**
     * The ring at {@code radius}, excluding the centre.
     *
     * <p>A cell is on the ring when its distance from the centre <em>rounds</em>
     * to the radius. That gives a closed, eight-connected loop at every radius —
     * an annulus test with a fixed thickness does not, and produces circles with
     * gaps in them at the diagonals, which for a mod about closed rings would be
     * an unusually pointed bug.
     */
    public static List<PlaneOffset> ring(int radius) {
        int r = Math.clamp(radius, 0, MAX_RADIUS);
        if (r <= 0) {
            return List.of();
        }
        List<PlaneOffset> cells = new ArrayList<>();
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                if (Math.round(Math.sqrt((double) a * a + (double) b * b)) == r) {
                    cells.add(new PlaneOffset(a, b));
                }
            }
        }
        return List.copyOf(cells);
    }

    /** Core plus ring — every cell a structure of this radius occupies. */
    public static List<PlaneOffset> all(int radius) {
        List<PlaneOffset> cells = new ArrayList<>();
        cells.add(PlaneOffset.CENTRE);
        cells.addAll(ring(radius));
        return List.copyOf(cells);
    }
}