package com.sigils.core.spell;

import java.util.List;

/** The result of validating a spell graph: valid, or a list of human-readable errors. */
public record ValidationResult(boolean valid, List<String> errors) {

    public ValidationResult {
        errors = List.copyOf(errors);
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failed(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}