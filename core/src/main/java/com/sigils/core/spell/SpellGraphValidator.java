package com.sigils.core.spell;

import java.util.ArrayList;
import java.util.List;

/** Checks that a spell graph is structurally castable. */
public final class SpellGraphValidator {

    private SpellGraphValidator() {}

    public static ValidationResult validate(SpellGraph graph) {
        List<String> errors = new ArrayList<>();

        if (graph.crests().isEmpty()) {
            errors.add("Spell has no crest — every spell needs at least one elemental core.");
        }
        if (graph.rings().isEmpty()) {
            errors.add("Spell has no enclosing ring — the circle must be closed to cast.");
        }

        boolean[] attached = new boolean[graph.modifiers().size()];
        for (SpellGraph.Edge edge : graph.edges()) {
            if (edge.modifierIndex() < 0 || edge.modifierIndex() >= graph.modifiers().size()) {
                errors.add("An edge references a modifier that does not exist (" + edge.modifierIndex() + ").");
                continue;
            }
            if (edge.crestIndex() < 0 || edge.crestIndex() >= graph.crests().size()) {
                errors.add("An edge references a crest that does not exist (" + edge.crestIndex() + ").");
                continue;
            }
            attached[edge.modifierIndex()] = true;
        }
        for (int m = 0; m < attached.length; m++) {
            if (!attached[m]) {
                errors.add("Modifier " + m + " is not connected to any crest.");
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failed(errors);
    }
}