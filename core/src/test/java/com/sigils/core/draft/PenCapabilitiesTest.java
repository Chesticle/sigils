package com.sigils.core.draft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PenCapabilitiesTest {

    @Test
    @DisplayName("a plain pen carries the canvas limits and no character")
    void plainIsNeutral() {
        PenCapabilities pen = PenCapabilities.plain(DraftLimits.DRAFTING_TABLE);

        assertTrue(pen.neutral());
        assertEquals(DraftLimits.DRAFTING_TABLE.maxComplexity(), pen.limits().maxComplexity());
        assertFalse(pen.unlocks("sigils:restricted"));
    }

    @Test
    @DisplayName("nonsense from a datapack is clamped, not propagated")
    void constructorClampsBadValues() {
        PenCapabilities pen = new PenCapabilities(
                DraftLimits.NOTEPAD, -3f, 7f, false, 0, 0, Set.of());

        assertEquals(0f, pen.instabilityFactor(), 1e-5);
        assertEquals(1f, pen.instabilityFloor(), 1e-5);
    }

    @Test
    @DisplayName("the unlocked tag set is a defensive copy")
    void unlockedTagsAreCopied() {
        Set<String> tags = new HashSet<>(Set.of("sigils:restricted"));
        PenCapabilities pen = new PenCapabilities(
                DraftLimits.DRAFTING_TABLE, 1f, 0f, false, 0, 0, tags);

        tags.clear();
        assertTrue(pen.unlocks("sigils:restricted"));
    }
}