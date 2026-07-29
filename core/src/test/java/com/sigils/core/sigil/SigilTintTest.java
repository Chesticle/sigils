package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SigilTintTest {

    private static final int MAGICAL = 0x2A2440;   // Phase 5A's magical ink
    private static final int NETHERITE = 0x12100F; // Phase 5B's netherite ink

    private static int r(int argb) { return (argb >> 16) & 0xFF; }
    private static int g(int argb) { return (argb >> 8) & 0xFF; }
    private static int b(int argb) { return argb & 0xFF; }

    @Test
    @DisplayName("the alpha byte is always set")
    void alphaIsAlwaysOpaque() {
        assertEquals(0xFF, (SigilTint.decal(MAGICAL, 1f) >>> 24) & 0xFF);
        assertEquals(0xFF, (SigilTint.decal(0x000000, 0f) >>> 24) & 0xFF);
    }

    @Test
    @DisplayName("a very dark ink still lands in readable range")
    void darkInkIsLiftedIntoView() {
        int shown = SigilTint.decal(NETHERITE, 1f);

        // Not a specific value — a floor. The bug this catches is "renders black".
        assertTrue(r(shown) > 60, "red was " + r(shown));
        assertTrue(g(shown) > 60, "green was " + g(shown));
        assertTrue(b(shown) > 60, "blue was " + b(shown));
    }

    @Test
    @DisplayName("the ink's hue survives the lift")
    void hueIsPreserved() {
        int shown = SigilTint.decal(MAGICAL, 1f);

        // #2A2440 is blue-dominant and green-poorest. It should stay that way.
        assertTrue(b(shown) > r(shown), "blue should still lead");
        assertTrue(r(shown) > g(shown), "green should still trail");
    }

    @Test
    @DisplayName("scaling preserves the ink's channel ratios exactly")
    void channelRatiosSurvive() {
        int shown = SigilTint.decal(MAGICAL, 1f);

        // #2A2440 is blue:red = 64:42. Whatever brightness we land on, that
        // ratio is the ink's identity and must come through unchanged.
        assertEquals(64f / 42f, (float) b(shown) / r(shown), 0.05f);
    }

    @Test
    @DisplayName("two dark inks do not collapse into the same grey")
    void inksStayDistinguishable() {
        int magical = SigilTint.decal(MAGICAL, 1f);
        int netherite = SigilTint.decal(NETHERITE, 1f);

        assertTrue(b(magical) - r(magical) > 40, "magical should read as violet");
        assertTrue(Math.abs(b(netherite) - r(netherite)) < 40, "netherite should read as neutral");
    }

    @Test
    @DisplayName("wear dims the mark without ever blacking it out")
    void wearDimsButDoesNotErase() {
        int fresh = SigilTint.decal(MAGICAL, 1f);
        int worn = SigilTint.decal(MAGICAL, 0.5f);
        int dead = SigilTint.decal(MAGICAL, 0f);

        assertTrue(b(worn) < b(fresh), "half-worn should be dimmer");
        assertTrue(b(dead) < b(worn), "spent should be dimmer still");
        assertTrue(b(dead) > 0, "but never invisible");
    }
}