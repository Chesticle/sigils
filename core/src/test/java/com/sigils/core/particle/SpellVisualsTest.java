package com.sigils.core.particle;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.reaction.Resolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpellVisualsTest {

    // Pure red and pure blue; every non-colour axis is identical so the blend's
    // colour is the only thing that moves.
    private static final ParticleProfile RED = new ParticleProfile(
            1f, 0f, 0f, 0.2f, 0f, 10f, 0f, 0.1f, 0f, 0f, 0f, 0f, 1f, 0f);
    private static final ParticleProfile BLUE = new ParticleProfile(
            0f, 0f, 1f, 0.2f, 0f, 10f, 0f, 0.1f, 0f, 0f, 0f, 0f, 1f, 0f);

    private static ProfileLookup lookupOf(Map<String, ParticleProfile> m) {
        return m::get; // Map.get returns null for a missing id — exactly the contract
    }

    @Test
    @DisplayName("a single phenomenon blends to its own profile")
    void singlePhenomenon() {
        Resolution res = new Resolution(Map.of("sigils:combustion", 1f), ElementalMixture.EMPTY);
        ParticleProfile out = SpellVisuals.blend(res,
                lookupOf(Map.of("sigils:combustion", RED))).orElseThrow();
        assertEquals(1f, out.red(), 1e-5);
        assertEquals(0f, out.blue(), 1e-5);
    }

    @Test
    @DisplayName("equal phenomenon + residual weights land halfway between the profiles")
    void equalWeightsBlendToMidpoint() {
        // one phenomenon (red, weight 1) and one leftover element (blue, weight 1)
        Resolution res = new Resolution(
                Map.of("sigils:combustion", 1f),
                ElementalMixture.of("sigils:water", 1f));
        ParticleProfile out = SpellVisuals.blend(res,
                lookupOf(Map.of("sigils:combustion", RED, "sigils:water", BLUE))).orElseThrow();
        assertEquals(0.5f, out.red(), 1e-5);
        assertEquals(0.5f, out.blue(), 1e-5);
    }

    @Test
    @DisplayName("a fire-heavy mix leans toward the fire profile — the slider, proven")
    void weightsBiasTheBlend() {
        // combustion weight 3 (red) vs leftover water weight 1 (blue) => 0.75 / 0.25
        Resolution res = new Resolution(
                Map.of("sigils:combustion", 3f),
                ElementalMixture.of("sigils:water", 1f));
        ParticleProfile out = SpellVisuals.blend(res,
                lookupOf(Map.of("sigils:combustion", RED, "sigils:water", BLUE))).orElseThrow();
        assertEquals(0.75f, out.red(), 1e-5);
        assertEquals(0.25f, out.blue(), 1e-5);
    }

    @Test
    @DisplayName("an inert resolution has no visual")
    void inertHasNoVisual() {
        Resolution res = new Resolution(Map.of(), ElementalMixture.EMPTY);
        assertTrue(SpellVisuals.blend(res, lookupOf(Map.of())).isEmpty());
    }

    @Test
    @DisplayName("ids with no registered profile are skipped, not fatal")
    void unknownIdsSkipped() {
        Resolution res = new Resolution(
                Map.of("sigils:mystery", 1f),
                ElementalMixture.of("sigils:water", 1f));
        ParticleProfile out = SpellVisuals.blend(res,
                lookupOf(Map.of("sigils:water", BLUE))).orElseThrow();
        assertEquals(1f, out.blue(), 1e-5); // only water contributed a profile
    }
}