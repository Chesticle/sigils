package com.sigils.core.draft;

import java.util.Objects;
import java.util.Set;

/**
 * What a pen can do, beyond what the canvas will accept.
 *
 * <p>Wraps {@link DraftLimits} rather than repeating it: the three fields the
 * roadmap's {@code PenCapabilities} shares with the canvas limits have been in
 * {@code DraftLimits} since Phase 4A, and every caller already reads them from
 * there. This record adds only what a tier needs that a canvas doesn't.
 *
 * @param limits              placements, crests, complexity, radius, rings
 * @param instabilityFactor   multiplier on (1 - fidelity); 1.0 is neutral,
 *                            below 1 forgives a shaky hand, above 1 punishes it
 * @param instabilityFloor    instability a perfect trace still suffers — a
 *                            feather quill wobbles no matter who holds it
 * @param inklessOnSolids     may draw world sigils without spending ink (Phase 6)
 * @param maxWorldSigilRadius largest world sigil this pen can lay out (Phase 6)
 * @param maxArtifactTier     which armour materials it can inscribe (Phase 8)
 * @param unlockedTags        glyph tags this pen makes usable (Phase 9)
 */
public record PenCapabilities(
        DraftLimits limits,
        float instabilityFactor,
        float instabilityFloor,
        boolean inklessOnSolids,
        int maxWorldSigilRadius,
        int maxArtifactTier,
        Set<String> unlockedTags
) {
    public PenCapabilities {
        Objects.requireNonNull(limits, "limits");
        unlockedTags = Set.copyOf(unlockedTags);
        instabilityFactor = Math.max(0f, instabilityFactor);
        instabilityFloor = Math.clamp(instabilityFloor, 0f, 1f);
    }

    /**
     * A pen with no drawbacks and no privileges — the identity tier. Useful as a
     * fallback when there is no pen at all and the screen still needs a canvas
     * radius to draw.
     */
    public static PenCapabilities plain(DraftLimits limits) {
        return new PenCapabilities(limits, 1f, 0f, false, 0, 0, Set.of());
    }

    /** Whether this pen makes a restricted glyph tag usable. */
    public boolean unlocks(String tag) {
        return unlockedTags.contains(tag);
    }

    /** True if this pen changes nothing about how a trace scores. */
    public boolean neutral() {
        return instabilityFactor == 1f && instabilityFloor == 0f;
    }
}