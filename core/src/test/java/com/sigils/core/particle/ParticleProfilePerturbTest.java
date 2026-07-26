package com.sigils.core.particle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleProfilePerturbTest {

    private static final ParticleProfile VIVID = new ParticleProfile(
            1f, 0f, 0f,    // pure red
            0.2f, 0.1f,    // size, sizeJitter
            12f, 2f,       // lifetime, lifetimeJitter
            0.1f, 0.1f,    // speed, speedSpread
            0f, 0.1f,      // gravity, turbulence
            0.5f, 1f, 0f); // emissive, density, trailLength

    @Test
    @DisplayName("zero instability is a no-op (same instance back)")
    void zeroIsIdentity() {
        assertSame(VIVID, VIVID.perturbed(0f));
    }

    @Test
    @DisplayName("instability raises turbulence and every jitter axis")
    void roughensMotion() {
        ParticleProfile p = VIVID.perturbed(1f);
        assertTrue(p.turbulence() > VIVID.turbulence(), "turbulence should climb");
        assertTrue(p.sizeJitter() > VIVID.sizeJitter(), "size jitter should climb");
        assertTrue(p.speedSpread() > VIVID.speedSpread(), "speed spread should climb");
    }

    @Test
    @DisplayName("instability desaturates the colour toward grey")
    void desaturates() {
        ParticleProfile p = VIVID.perturbed(1f);
        assertTrue(p.red() < VIVID.red(), "red should fall toward grey");
        assertTrue(p.green() > VIVID.green(), "green should rise toward grey");
        assertTrue(p.blue() > VIVID.blue(), "blue should rise toward grey");
    }

    @Test
    @DisplayName("more instability desaturates more")
    void monotonicDesaturation() {
        assertTrue(saturation(VIVID.perturbed(1.0f)) < saturation(VIVID.perturbed(0.5f)),
                "1.0 should be greyer than 0.5");
    }

    private static float saturation(ParticleProfile p) {
        float max = Math.max(p.red(), Math.max(p.green(), p.blue()));
        float min = Math.min(p.red(), Math.min(p.green(), p.blue()));
        return max - min;
    }
}