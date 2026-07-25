package com.sigils.core.reaction;

import com.sigils.core.SigilsCore;
import com.sigils.core.element.ElementalMixture;

import java.util.Map;
import java.util.Objects;

/**
 * What the resolver produces from a mixture.
 *
 * @param phenomena reaction products, phenomenon id -&gt; total strength.
 *                  Iteration order is sorted by id for deterministic output.
 * @param residual  elements left unreacted. These still act on their own — the
 *                  "fire left over after the steam is made" that gives the
 *                  system its physical feel. Later phases turn residual into
 *                  its own effects and particles; Part A just tracks it.
 */
public record Resolution(Map<String, Float> phenomena, ElementalMixture residual) {

    public Resolution {
        Objects.requireNonNull(phenomena, "phenomena");
        Objects.requireNonNull(residual, "residual");
    }

    public boolean hasPhenomenon(String phenomenonId) {
        return strengthOf(phenomenonId) > SigilsCore.EPSILON;
    }

    public float strengthOf(String phenomenonId) {
        return phenomena.getOrDefault(phenomenonId, 0f);
    }

    /** True if no reaction fired at all. */
    public boolean isInert() {
        return phenomena.isEmpty();
    }
}