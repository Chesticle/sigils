package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PollScheduleTest {

    @Test
    @DisplayName("interval 0 is never due; interval 1 is always due")
    void degenerateIntervals() {
        assertFalse(PollSchedule.due(17, 12345, 0));
        assertFalse(PollSchedule.due(17, 12345, -4));
        assertTrue(PollSchedule.due(17, 12345, 1));
    }

    @Test
    @DisplayName("a position comes up exactly once per interval, negative hashes included")
    void exactlyOncePerInterval() {
        for (int hash : new int[] {0, 7, -7, Integer.MIN_VALUE}) {
            int hits = 0;
            for (long t = 0; t < 40; t++) {
                if (PollSchedule.due(t, hash, 4)) {
                    hits++;
                }
            }
            assertEquals(10, hits, "hash " + hash + " should be due 10 times in 40 ticks");
        }
    }

    @Test
    @DisplayName("different positions land on different ticks")
    void workIsSpreadAcrossTheInterval() {
        // Four hashes that differ by one each: one per tick of a 4-tick interval.
        long t = 100;
        int due = 0;
        for (int hash = 0; hash < 4; hash++) {
            if (PollSchedule.due(t, hash, 4)) {
                due++;
            }
        }
        assertEquals(1, due, "a quarter of the work, not all of it");
    }
}