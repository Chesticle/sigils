package com.sigils.core.draft;

import com.sigils.core.spell.CompiledSpell;

/**
 * How the tools used to draft a spell change the trace quality recorded on it.
 *
 * <p>Applied once, when the spell is inscribed — a parchment is cast long after
 * the pen has been put away, so the pen's character has to be folded into what
 * gets written down.
 */
public final class DraftQuality {

    private DraftQuality() {}

    /**
     * The fidelity that will actually be stored, given the tools.
     *
     * @param tracedFidelity   what the player's hand earned, 0..1
     * @param pen              the pen in the table
     * @param parchmentQuality multiplier on the traced fidelity; 1.0 is neutral,
     *                         above 1 forgives, below 1 blurs
     */
    public static float effectiveFidelity(
            float tracedFidelity, PenCapabilities pen, float parchmentQuality) {

        float onPaper = Math.clamp(tracedFidelity * parchmentQuality, 0f, 1f);

        // The same arithmetic as CompiledSpell.instabilityWith(...), then floored:
        // a coarse pen wobbles even when the hand did not.
        float instability = Math.clamp((1f - onPaper) * pen.instabilityFactor(), 0f, 1f);
        instability = Math.max(instability, pen.instabilityFloor());

        return 1f - Math.clamp(instability, 0f, 1f);
    }

    /**
     * A copy of {@code traced} with its fidelity rewritten for the tools used.
     * Everything else — mixture, delivery, rings, schema version — is untouched:
     * the tools change how well the spell was recorded, never what it is.
     */
    public static CompiledSpell stamp(
            CompiledSpell traced, PenCapabilities pen, float parchmentQuality) {

        float fidelity = effectiveFidelity(traced.fidelity(), pen, parchmentQuality);
        if (fidelity == traced.fidelity()) {
            return traced;
        }
        return new CompiledSpell(
                traced.schemaVersion(),
                traced.mixture(),
                traced.delivery(),
                fidelity,
                traced.rings());
    }
}