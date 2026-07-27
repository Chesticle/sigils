package com.sigils.core.draft;

import com.sigils.core.SigilsCore;

/**
 * A running ink balance for one drafting session.
 *
 * <p>Charged incrementally as the player traces, so the bar drains in real time
 * and a stroke that outruns the supply is refused at the exact point the ink
 * ran out — rather than the whole spell failing after the fact.
 */
public final class InkLedger {

    private final float capacity;
    private float spent;

    public InkLedger(float capacity) {
        if (capacity < 0f) {
            throw new IllegalArgumentException("Ink capacity cannot be negative");
        }
        this.capacity = capacity;
    }

    public float capacity() {
        return capacity;
    }

    public float spent() {
        return spent;
    }

    public float remaining() {
        return capacity - spent;
    }

    /** 0..1, for the ink bar. */
    public float fractionRemaining() {
        return capacity <= 0f ? 0f : Math.clamp(remaining() / capacity, 0f, 1f);
    }

    public boolean dry() {
        return remaining() <= SigilsCore.EPSILON;
    }

    /**
     * Spends ink. Returns false and spends <em>nothing</em> if the balance won't
     * cover it — an all-or-nothing charge, so a refused stroke leaves no
     * half-drained state behind.
     */
    public boolean charge(float amount) {
        if (amount < 0f) {
            throw new IllegalArgumentException("Ink charge cannot be negative");
        }
        if (amount > remaining() + SigilsCore.EPSILON) {
            return false;
        }
        spent += amount;
        return true;
    }
}