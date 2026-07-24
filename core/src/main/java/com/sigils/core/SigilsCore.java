package com.sigils.core;

/**
 * Constants shared by the entire spell engine.
 *
 * <p>This module is pure Java: it must never import anything from
 * {@code net.minecraft} or {@code net.neoforged}. Everything here has to be
 * unit-testable without launching the game.
 */
public final class SigilsCore {

    private SigilsCore() {}

    /** Namespace used for built-in content. */
    public static final String NAMESPACE = "sigils";

    /**
     * Schema version for any spell serialised to disk (parchment, sketchbook,
     * inscribed armour, placed world sigil).
     *
     * <p>Bump this whenever the persisted shape of a compiled spell changes, and
     * add a migration step. Players losing their spellbooks to an update is the
     * fastest way to lose a community.
     */
    public static final int SPELL_SCHEMA_VERSION = 1;

    /**
     * Amounts below this are treated as zero throughout the engine.
     *
     * <p>Without this, floating point residue accumulates and a reaction that
     * should consume its reagents exactly leaves a ghost of 0.0000001 fire
     * behind, which then renders sparks nobody asked for.
     */
    public static final float EPSILON = 1.0e-5f;
}