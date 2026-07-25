package com.sigils.core.spell;

import com.sigils.core.element.ElementalMixture;

import java.util.List;
import java.util.Objects;

/**
 * A ready-to-cast spell. {@code mixture} feeds the reaction resolver; {@code
 * delivery} says how it's projected; {@code fidelity} (0..1) is the aggregate
 * trace quality that drives instability.
 *
 * <p>Carries {@code schemaVersion} because it is persisted (spells saved to
 * parchment / artifacts). {@code fidelity} is the hook later phases tick down as
 * artifacts wear, and pens scale via {@link #instabilityWith(float)}.
 */
public record CompiledSpell(
        int schemaVersion,
        ElementalMixture mixture,
        Delivery delivery,
        float fidelity,
        List<String> rings
) {
    public CompiledSpell {
        Objects.requireNonNull(mixture, "mixture");
        Objects.requireNonNull(delivery, "delivery");
        rings = List.copyOf(rings);
    }

    /** Instability from tracing quality alone, before any pen factor. */
    public float baseInstability() {
        return Math.clamp(1f - fidelity, 0f, 1f);
    }

    /** Instability once a pen's instability multiplier is applied at cast time. */
    public float instabilityWith(float penFactor) {
        return Math.clamp(baseInstability() * penFactor, 0f, 1f);
    }
}