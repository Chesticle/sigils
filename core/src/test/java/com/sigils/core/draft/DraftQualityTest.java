package com.sigils.core.draft;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.core.spell.Delivery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DraftQualityTest {

    private static PenCapabilities pen(float factor, float floor) {
        return new PenCapabilities(
                DraftLimits.DRAFTING_TABLE, factor, floor, false, 0, 0, Set.of());
    }

    private static CompiledSpell spellWith(float fidelity) {
        return new CompiledSpell(
                1,
                ElementalMixture.of("sigils:fire", 2f),
                new Delivery("sigils:beam", 1f, 0, "sigils:looked_at_block"),
                fidelity,
                List.of("sigils:ring_basic"));
    }

    @Test
    @DisplayName("a neutral pen and plain parchment change nothing")
    void identityToolsAreIdentity() {
        assertEquals(0.8f, DraftQuality.effectiveFidelity(0.8f, pen(1f, 0f), 1f), 1e-5);
    }

    @Test
    @DisplayName("a feather quill wobbles even on a flawless trace")
    void floorAppliesToAPerfectTrace() {
        // 1.0 traced -> 0 instability -> raised to the pen's floor of 0.15.
        assertEquals(0.85f, DraftQuality.effectiveFidelity(1f, pen(1f, 0.15f), 1f), 1e-5);
    }

    @Test
    @DisplayName("a coarse pen magnifies a shaky hand")
    void factorAboveOnePunishes() {
        // 0.6 traced -> 0.4 instability -> x1.5 -> 0.6 -> fidelity 0.4.
        assertEquals(0.4f, DraftQuality.effectiveFidelity(0.6f, pen(1.5f, 0f), 1f), 1e-5);
    }

    @Test
    @DisplayName("a fine nib rescues the same hand")
    void factorBelowOneForgives() {
        // 0.6 traced -> 0.4 instability -> x0.5 -> 0.2 -> fidelity 0.8.
        assertEquals(0.8f, DraftQuality.effectiveFidelity(0.6f, pen(0.5f, 0f), 1f), 1e-5);
    }

    @Test
    @DisplayName("fine parchment holds the line; coarse parchment blurs it")
    void parchmentQualityMovesFidelityBothWays() {
        assertEquals(0.99f, DraftQuality.effectiveFidelity(0.9f, pen(1f, 0f), 1.1f), 1e-5);
        assertEquals(0.80f, DraftQuality.effectiveFidelity(1.0f, pen(1f, 0f), 0.8f), 1e-5);
    }

    @Test
    @DisplayName("nothing can push fidelity outside 0..1")
    void resultIsAlwaysClamped() {
        assertEquals(0f, DraftQuality.effectiveFidelity(0.5f, pen(8f, 0f), 1f), 1e-5);
        assertEquals(1f, DraftQuality.effectiveFidelity(1f, pen(1f, 0f), 4f), 1e-5);
        assertEquals(0f, DraftQuality.effectiveFidelity(1f, pen(1f, 1f), 1f), 1e-5);
    }

    @Test
    @DisplayName("stamping rewrites the fidelity and nothing else")
    void stampTouchesOnlyFidelity() {
        CompiledSpell traced = spellWith(1f);
        CompiledSpell stamped = DraftQuality.stamp(traced, pen(1f, 0.2f), 1f);

        assertEquals(0.8f, stamped.fidelity(), 1e-5);
        assertEquals(traced.schemaVersion(), stamped.schemaVersion());
        assertEquals(traced.mixture(), stamped.mixture());
        assertEquals(traced.delivery(), stamped.delivery());
        assertEquals(traced.rings(), stamped.rings());
    }

    @Test
    @DisplayName("a neutral pen returns the very same spell object")
    void stampIsIdentityWhenNothingChanges() {
        CompiledSpell traced = spellWith(0.73f);
        assertSame(traced, DraftQuality.stamp(traced, pen(1f, 0f), 1f));
    }
}