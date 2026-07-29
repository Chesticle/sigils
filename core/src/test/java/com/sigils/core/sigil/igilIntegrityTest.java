package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SigilIntegrityTest {

    @Test
    @DisplayName("integrity is clamped to 0..1 rather than trusted")
    void clampedToRange() {
        assertEquals(1f, new SigilIntegrity(4f).value(), 1e-6);
        assertEquals(0f, new SigilIntegrity(-4f).value(), 1e-6);
    }

    @Test
    @DisplayName("three buckets take a fresh sigil inert")
    void threeBucketsGoInert() {
        SigilIntegrity sigil = SigilIntegrity.FULL
                .washed(SigilIntegrity.WASH_BUCKET)
                .washed(SigilIntegrity.WASH_BUCKET);

        assertFalse(sigil.inert(), "two should not be enough");
        assertTrue(sigil.washed(SigilIntegrity.WASH_BUCKET).inert());
    }

    @Test
    @DisplayName("a sponge is one touch")
    void spongeIsOneTouch() {
        assertTrue(SigilIntegrity.FULL.washed(SigilIntegrity.WASH_SPONGE).inert());
    }

    @Test
    @DisplayName("permanent ink does not weather")
    void permanentInkIgnoresWeather() {
        SigilIntegrity sigil = SigilIntegrity.FULL;
        for (int i = 0; i < 500; i++) {
            sigil = sigil.weathered(true);
        }
        assertEquals(1f, sigil.value(), 1e-6);
    }

    @Test
    @DisplayName("impermanent ink weathers away in a finite number of steps")
    void impermanentInkWeathersAway() {
        SigilIntegrity sigil = SigilIntegrity.FULL;
        int steps = 0;
        while (!sigil.inert() && steps < 1000) {
            sigil = sigil.weathered(false);
            steps++;
        }
        assertTrue(sigil.inert(), "should have worn out");
        assertEquals(48, steps, "1.0 -> 0.05 at 0.02 a step");
    }

    @Test
    @DisplayName("the three bands don't overlap")
    void bandsAreDistinct() {
        assertTrue(SigilIntegrity.FULL.intact());
        assertFalse(SigilIntegrity.FULL.inert());

        SigilIntegrity worn = new SigilIntegrity(0.5f);
        assertFalse(worn.intact(), "half-gone is not recoverable");
        assertFalse(worn.inert(), "half-gone still works");

        SigilIntegrity dead = new SigilIntegrity(0.01f);
        assertTrue(dead.inert());
        assertFalse(dead.intact());
    }

    @Test
    @DisplayName("wear steps run 0 at full to WEAR_STEPS when inert")
    void wearStepsSpanTheRange() {
        assertEquals(0, SigilIntegrity.FULL.wearStep());
        assertEquals(1, new SigilIntegrity(0.75f).wearStep());
        assertEquals(2, new SigilIntegrity(0.5f).wearStep());
        assertEquals(3, new SigilIntegrity(0.25f).wearStep());
        assertEquals(SigilIntegrity.WEAR_STEPS, new SigilIntegrity(0f).wearStep());
    }

    @Test
    @DisplayName("only an inert sigil gets the last wear step")
    void theLastStepMeansDead() {
        // 0.1 rounds to zero remaining but is still alive, so it must not claim
        // the step that means "dead".
        SigilIntegrity nearlyGone = new SigilIntegrity(0.1f);

        assertFalse(nearlyGone.inert());
        assertEquals(SigilIntegrity.WEAR_STEPS - 1, nearlyGone.wearStep());
    }

    @Test
    @DisplayName("remainingAt inverts wearStep well enough to render with")
    void remainingAtIsTheInverse() {
        assertEquals(1f, SigilIntegrity.remainingAt(0), 1e-6);
        assertEquals(0.5f, SigilIntegrity.remainingAt(2), 1e-6);
        assertEquals(0f, SigilIntegrity.remainingAt(SigilIntegrity.WEAR_STEPS), 1e-6);
    }

    @Test
    @DisplayName("a worn sigil casts less stably, and never divides by zero")
    void instabilityGrowsAsItWears() {
        assertEquals(1f, SigilIntegrity.FULL.instabilityFactor(), 1e-5);
        assertEquals(2f, new SigilIntegrity(0.5f).instabilityFactor(), 1e-5);
        assertTrue(Float.isFinite(new SigilIntegrity(0f).instabilityFactor()));
    }
}