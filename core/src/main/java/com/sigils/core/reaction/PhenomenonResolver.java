package com.sigils.core.reaction;

import com.sigils.core.SigilsCore;
import com.sigils.core.element.ElementalMixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Applies reaction rules to a mixture using limiting-reagent chemistry.
 *
 * <p>The algorithm:
 * <ol>
 *   <li>Sort rules by priority (descending), then id (ascending) for
 *       determinism.</li>
 *   <li>Make a pass over every rule. For each, work out how far it can proceed
 *       given what's currently in the pool (the limiting reagent), consume that
 *       much, and emit the corresponding phenomenon strength.</li>
 *   <li>Repeat passes until a full pass changes nothing.</li>
 * </ol>
 *
 * <p>Because reactions only ever <em>remove</em> elements from a finite pool,
 * the pool strictly shrinks and the process terminates. A hard pass cap guards
 * against floating-point edge cases.
 *
 * <p>The resolver is stateless — one instance can serve every cast on the
 * server. It is also pure: same mixture plus same rules always gives the same
 * result. That determinism is what makes spells reproducible and bugs
 * traceable.
 */
public final class PhenomenonResolver {

    /** Safety cap. A well-formed rule set terminates in far fewer passes. */
    private static final int MAX_PASSES = 64;

    public Resolution resolve(ElementalMixture input, List<ReactionRule> rules) {
        List<ReactionRule> ordered = new ArrayList<>(rules);
        ordered.sort(Comparator.comparingInt(ReactionRule::priority).reversed()
                .thenComparing(ReactionRule::id));

        ElementalMixture residual = input;
        Map<String, Float> phenomena = new TreeMap<>(); // sorted keys => deterministic

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean progressed = false;

            for (ReactionRule rule : ordered) {
                float amount = reactionAmount(rule, residual);
                if (amount <= SigilsCore.EPSILON) {
                    continue;
                }

                // Tentatively consume the reagents.
                ElementalMixture next = residual;
                for (Reagent reagent : rule.inputs()) {
                    next = next.minus(
                            ElementalMixture.of(reagent.elementId(), reagent.consumption() * amount));
                }

                // Only count this as progress if something was actually consumed.
                // Guards against a degenerate rule spinning without changing the pool.
                if (next.equals(residual)) {
                    continue;
                }

                residual = next;
                phenomena.merge(rule.outputPhenomenon(), rule.yield() * amount, Float::sum);
                progressed = true;
            }

            if (!progressed) {
                break;
            }
        }

        return new Resolution(Collections.unmodifiableMap(phenomena), residual);
    }

    /**
     * How far a rule can proceed against the current pool.
     *
     * <p>Returns 0 if any reagent is absent or sits outside its ratio window.
     * Otherwise returns the limiting-reagent amount: the smallest
     * {@code available / consumption} across all reagents. That's the classic
     * limiting reagent — the input that runs out first caps the reaction.
     */
    private float reactionAmount(ReactionRule rule, ElementalMixture residual) {
        float total = residual.total();
        if (total <= SigilsCore.EPSILON) {
            return 0f;
        }

        float limiting = Float.MAX_VALUE;
        for (Reagent reagent : rule.inputs()) {
            float available = residual.amountOf(reagent.elementId());
            if (available <= SigilsCore.EPSILON) {
                return 0f; // a required reagent is missing
            }
            float ratio = available / total;
            if (!reagent.ratioInWindow(ratio)) {
                return 0f; // present, but the proportion is wrong for this reaction
            }
            limiting = Math.min(limiting, available / reagent.consumption());
        }
        return limiting == Float.MAX_VALUE ? 0f : limiting;
    }
}