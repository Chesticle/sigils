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
    @DisplayName("a worn sigil casts less stably, and never divides by zero")
    void instabilityGrowsAsItWears() {
        assertEquals(1f, SigilIntegrity.FULL.instabilityFactor(), 1e-5);
        assertEquals(2f, new SigilIntegrity(0.5f).instabilityFactor(), 1e-5);
        assertTrue(Float.isFinite(new SigilIntegrity(0f).instabilityFactor()));
    }
}