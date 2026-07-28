package com.sigils.core.sigil;

/**
 * How much of a drawn sigil is still there.
 *
 * <p>1.0 is freshly inked; below {@link #INERT_AT} the lines are too broken to
 * carry a circuit and the sigil does nothing at all. Everything in between casts,
 * badly — see {@link #instabilityFactor()}.
 *
 * <p>Immutable, and every transition returns a new instance, because this ends up
 * in a block entity's saved data and shared mutable state there is how two chunks
 * end up disagreeing about the same sigil.
 *
 * @param value 0..1, clamped on construction
 */
public record SigilIntegrity(float value) {

    /** Freshly drawn. */
    public static final SigilIntegrity FULL = new SigilIntegrity(1f);

    /** At or below this, the sigil is inert — present, visible, and dead. */
    public static final float INERT_AT = 0.05f;

    /** One splash from a water bucket. Three of them finish the job. */
    public static final float WASH_BUCKET = 0.34f;

    /** A sponge takes the whole thing, which is the roadmap's "cleanly disables one". */
    public static final float WASH_SPONGE = 1f;

    /** One step of weather on an uncovered, impermanent sigil. */
    public static final float WEATHER_STEP = 0.02f;

    public SigilIntegrity {
        // Clamp rather than throw: this is rebuilt from saved data that may have
        // been written by an older build or edited by hand.
        value = Math.clamp(value, 0f, 1f);
    }

    /** True when the sigil can no longer carry a circuit. */
    public boolean inert() {
        return value <= INERT_AT;
    }

    /** Take {@code amount} off. Negative amounts do nothing rather than repairing. */
    public SigilIntegrity washed(float amount) {
        return amount <= 0f ? this : new SigilIntegrity(value - amount);
    }

    /**
     * One step of weather.
     *
     * <p>The permanence check lives here rather than at the call site so that the
     * rule — <em>permanent ink does not weather</em> — is stated once, in the place
     * that gets unit-tested. Part C reads the flag off the sigil's recorded
     * {@code sigils:ink_grade} and passes it straight in.
     */
    public SigilIntegrity weathered(boolean permanentInk) {
        return permanentInk ? this : washed(WEATHER_STEP);
    }

    /**
     * What a worn sigil does to the spell it carries.
     *
     * <p>This is the argument for {@link com.sigils.core.spell.CompiledSpell}'s
     * {@code instabilityWith(float)}, which has been sitting unused since Phase 1B
     * waiting for exactly this: a factor applied at *cast* time rather than at
     * inscribe time, because unlike a pen, the sigil is still there when it fires.
     *
     * <p>1.0 at full integrity, 2.0 at half, and bounded by {@link #INERT_AT} so it
     * can't divide by zero on a sigil that's one tick from inert.
     */
    public float instabilityFactor() {
        return 1f / Math.max(value, INERT_AT);
    }

    /** {@code value} as a whole number of steps out of {@code max}, for bars and tints. */
    public int steps(int max) {
        return Math.round(value * max);
    }
}