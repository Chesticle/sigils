package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitLatchTest {

    private static final int CD = CircuitLatch.DEFAULT_COOLDOWN_TICKS;

    @Test
    @DisplayName("fires on the rising edge")
    void firesOnRisingEdge() {
        CircuitLatch latch = new CircuitLatch();

        assertFalse(latch.advance(false, 0, CD));
        assertTrue(latch.advance(true, 1, CD));
    }

    @Test
    @DisplayName("does not fire again while the circuit is held closed")
    void doesNotFireWhileHeld() {
        CircuitLatch latch = new CircuitLatch();
        assertTrue(latch.advance(true, 0, CD));

        for (long t = 1; t < 200; t++) {
            assertFalse(latch.advance(true, t, CD), "fired again at t=" + t);
        }
    }

    @Test
    @DisplayName("fires again once the circuit reopens and the cooldown has passed")
    void refiresAfterReopening() {
        CircuitLatch latch = new CircuitLatch();
        assertTrue(latch.advance(true, 0, CD));

        latch.advance(false, CD, CD);
        assertTrue(latch.advance(true, CD, CD));
    }

    @Test
    @DisplayName("a reopen-and-close inside the cooldown is refused")
    void respectsCooldown() {
        CircuitLatch latch = new CircuitLatch();
        assertTrue(latch.advance(true, 0, CD));

        latch.advance(false, 5, CD);
        assertFalse(latch.advance(true, 6, CD), "6 ticks is inside a 20-tick cooldown");
    }

    @Test
    @DisplayName("time running backwards does not lock the sigil out forever")
    void timeGoingBackwardsDoesNotLockItOut() {
        CircuitLatch latch = new CircuitLatch();
        assertTrue(latch.advance(true, 1_000_000L, CD));

        // /time set 0
        latch.advance(false, 0, CD);
        assertTrue(latch.advance(true, 0, CD), "the deadline should have been reeled in");
    }

    @Test
    @DisplayName("a latch restored as closed does not fire on the first observation")
    void restoredClosedStateDoesNotFire() {
        CircuitLatch latch = new CircuitLatch();
        latch.restore(true, 0);

        assertFalse(latch.advance(true, 500, CD), "chunk load is not a rising edge");
    }
}