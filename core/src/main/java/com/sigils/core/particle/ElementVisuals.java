package com.sigils.core.particle;

import com.sigils.core.element.Element;

/**
 * Derives a {@link ParticleProfile} from an {@link Element}'s own fields — no
 * per-element rendering data, ever. Colour comes from {@code colorLinear},
 * lift/sink from {@code density} (negative rises, positive sinks — the same
 * sign the physics uses), glow from {@code luminance}. Adding an element is a
 * JSON file; its particles fall out of its numbers.
 */
public final class ElementVisuals {

    private ElementVisuals() {}

    public static ParticleProfile profileFor(Element element) {
        int c = element.colorLinear(); // already LINEAR — see the note in §5
        float r = ((c >> 16) & 0xFF) / 255f;
        float g = ((c >> 8) & 0xFF) / 255f;
        float b = (c & 0xFF) / 255f;

        float gravity = Math.clamp(element.density() * 0.05f, -0.2f, 0.2f);
        float emissive = Math.clamp(element.luminance(), 0f, 1f);

        return new ParticleProfile(
                r, g, b,
                0.15f, 0.05f,  // size, sizeJitter
                14f, 4f,       // lifetime, lifetimeJitter (ticks)
                0.10f, 0.03f,  // speed, speedSpread
                gravity,
                0.05f,         // turbulence — calm; instability adds the rest
                emissive,
                1.0f,          // density (spawn-rate multiplier)
                0.0f);         // trailLength
    }
}