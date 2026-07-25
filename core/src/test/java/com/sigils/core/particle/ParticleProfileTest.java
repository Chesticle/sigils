package com.sigils.core.particle;

import com.sigils.core.util.Weighted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParticleProfileTest {

    // Two clearly different profiles. Only the fields we assert on matter;
    // the rest are set so the blend arithmetic is easy to verify.
    private static final ParticleProfile RED = new ParticleProfile(
            1f, 0f, 0f, /*size*/ 1f, 0f, /*life*/ 10f, 0f, /*speed*/ 0f, 0f, 0f, 0f, 0f, /*density*/ 1f, 0f);

    private static final ParticleProfile BLUE = new ParticleProfile(
            0f, 0f, 1f, /*size*/ 3f, 0f, /*life*/ 30f, 0f, /*speed*/ 0f, 0f, 0f, 0f, 0f, /*density*/ 1f, 0f);

    @Test
    @DisplayName("a 50/50 blend is the midpoint of every field")
    void fiftyFifty() {
        ParticleProfile out = ParticleProfile.blend(List.of(
                Weighted.of(RED, 1f), Weighted.of(BLUE, 1f)));

        assertEquals(0.5f, out.red(), 1e-5);
        assertEquals(0.5f, out.blue(), 1e-5);
        assertEquals(2.0f, out.size(), 1e-5);     // (1 + 3) / 2
        assertEquals(20f, out.lifetime(), 1e-5);  // (10 + 30) / 2
    }

    @Test
    @DisplayName("weights bias the blend proportionally")
    void weightedBlend() {
        // 3 parts red, 1 part blue.
        ParticleProfile out = ParticleProfile.blend(List.of(
                Weighted.of(RED, 3f), Weighted.of(BLUE, 1f)));

        assertEquals(0.75f, out.red(), 1e-5);
        assertEquals(0.25f, out.blue(), 1e-5);
        assertEquals(1.5f, out.size(), 1e-5);     // (3*1 + 1*3) / 4
    }

    @Test
    @DisplayName("a single profile blends to itself")
    void singleInput() {
        ParticleProfile out = ParticleProfile.blend(List.of(Weighted.of(RED, 5f)));
        assertEquals(RED, out);
    }

    @Test
    @DisplayName("all-zero weights fall back to an even average instead of dividing by zero")
    void zeroWeightsFallBack() {
        ParticleProfile out = ParticleProfile.blend(List.of(
                Weighted.of(RED, 0f), Weighted.of(BLUE, 0f)));
        assertEquals(0.5f, out.red(), 1e-5);
        assertEquals(0.5f, out.blue(), 1e-5);
    }
}