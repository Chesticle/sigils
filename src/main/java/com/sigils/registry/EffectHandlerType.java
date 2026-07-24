package com.sigils.registry;

/**
 * A pluggable world-mutating behaviour (ignite, extinguish, push, place fluid...).
 *
 * <p>Phase 0 ships this as a stub with only a description, purely to prove that
 * code-backed registries work. Phase 2 gives it a real {@code apply(...)}
 * method that takes the resolved phenomenon and a delivery context.
 *
 * <p>Why a registry rather than an enum: addon mods (and future you) must be
 * able to add handlers without editing a switch statement. Nothing in the
 * engine may ever branch on a handler's identity.
 */
public interface EffectHandlerType {

    /** Human-readable description, shown by {@code /sigils handlers}. */
    String describe();

    /** Convenience factory for simple stub handlers. */
    static EffectHandlerType simple(String description) {
        return () -> description;
    }
}