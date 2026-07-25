package com.sigils.core.spell;

import java.util.List;

/** Either a compiled spell, or the reasons compilation failed. */
public sealed interface CompileResult {

    record Success(CompiledSpell spell) implements CompileResult {}

    record Failure(List<String> errors) implements CompileResult {
        public Failure {
            errors = List.copyOf(errors);
        }
    }
}