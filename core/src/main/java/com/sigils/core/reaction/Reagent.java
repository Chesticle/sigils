package com.sigils.core.reaction;

import com.sigils.core.SigilsCore;

import java.util.Objects;

/**
 * One input to a {@link ReactionRule}.
 *
 * <p>{@code consumption} is how much of {@code elementId} the reaction eats per
 * unit of reaction that proceeds. With two reagents at consumption 1.0 each,
 * one unit of reaction eats 1.0 of each.
 *
 * <p>The ratio window {@code [minRatio, maxRatio]} gates the reaction on
 * <em>proportion</em>, not just presence. A reagent with window {@code [0.4,
 * 0.6]} only lets its reaction fire when that element makes up 40–60% of the
 * mixture. The default window {@code [0, 1]} means "fire whenever present",
 * which is what the steam reaction wants.
 */
public record Reagent(String elementId, float minRatio, float maxRatio, float consumption) {

    public Reagent {
        Objects.requireNonNull(elementId, "elementId");
        if (consumption <= 0f) {
            throw new IllegalArgumentException("consumption must be > 0, got " + consumption);
        }
        if (minRatio < 0f || maxRatio > 1f || minRatio > maxRatio) {
            throw new IllegalArgumentException(
                    "ratio window must satisfy 0 <= min <= max <= 1, got [" + minRatio + ", " + maxRatio + "]");
        }
    }

    /** A reagent with no ratio gating: eligible whenever it is present at all. */
    public static Reagent of(String elementId, float consumption) {
        return new Reagent(elementId, 0f, 1f, consumption);
    }

    /** True if the given proportion (0..1) falls inside this reagent's window. */
    public boolean ratioInWindow(float ratio) {
        return ratio >= minRatio - SigilsCore.EPSILON
                && ratio <= maxRatio + SigilsCore.EPSILON;
    }
}