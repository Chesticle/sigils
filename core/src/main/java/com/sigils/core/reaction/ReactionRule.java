package com.sigils.core.reaction;

import java.util.List;
import java.util.Objects;

/**
 * A single rule of the magic system's chemistry.
 *
 * <p>This is <em>data</em>. In production these are loaded from datapack JSON
 * (wired up at the end of Part B). Nothing about the resolver cares where a rule
 * came from — which is exactly why the magic system is moddable: adding a
 * reaction is a JSON file, never a code change.
 *
 * @param id               namespaced id, e.g. {@code "sigils:steam"}
 * @param inputs           the reagents; must be non-empty
 * @param outputPhenomenon the phenomenon id this reaction emits
 * @param yield            phenomenon strength produced per unit of reaction
 * @param priority         higher priority rules get first claim on shared
 *                         reagents; ties break by id, so resolution is
 *                         deterministic regardless of file load order
 */
public record ReactionRule(
        String id,
        List<Reagent> inputs,
        String outputPhenomenon,
        float yield,
        int priority
) {
    public ReactionRule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(outputPhenomenon, "outputPhenomenon");
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Reaction '" + id + "' has no inputs");
        }
        if (yield <= 0f) {
            throw new IllegalArgumentException("Reaction '" + id + "' yield must be > 0, got " + yield);
        }
        inputs = List.copyOf(inputs);
    }
}