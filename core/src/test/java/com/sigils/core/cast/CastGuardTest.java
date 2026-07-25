package com.sigils.core.cast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CastGuardTest {

    @Test
    @DisplayName("the per-tick budget caps the number of casts")
    void tickBudgetCaps() {
        CastGuard guard = new CastGuard(8, 3);
        assertTrue(guard.tryBeginCast());
        assertTrue(guard.tryBeginCast());
        assertTrue(guard.tryBeginCast());
        assertFalse(guard.tryBeginCast(), "the 4th cast this tick must be refused");
    }

    @Test
    @DisplayName("resetting the tick restores the budget")
    void resetRestoresBudget() {
        CastGuard guard = new CastGuard(8, 1);
        assertTrue(guard.tryBeginCast());
        assertFalse(guard.tryBeginCast());
        guard.resetTick();
        assertTrue(guard.tryBeginCast(), "a new tick should allow casting again");
    }

    @Test
    @DisplayName("depth is limited, and negative depth is never valid")
    void depthLimit() {
        CastGuard guard = new CastGuard(3, 256);
        assertTrue(guard.withinDepth(0));
        assertTrue(guard.withinDepth(3));
        assertFalse(guard.withinDepth(4), "one past the max must be refused");
        assertFalse(guard.withinDepth(-1));
    }
}