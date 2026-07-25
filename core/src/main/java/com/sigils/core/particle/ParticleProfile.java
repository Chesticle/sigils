package com.sigils.core.particle;

import com.sigils.core.util.Weighted;

import java.util.List;

/**
 * A description of how a phenomenon looks, as pure numbers. The renderer (Phase
 * 3) reads these; the core only knows how to blend them.
 *
 * <p>Colour is linear RGB. {@code jitter}/{@code spread} fields randomise
 * per-particle at spawn (the only randomness in the visual, and purely
 * cosmetic). {@code density} scales how many particles spawn.
 */
public record ParticleProfile(
        float red, float green, float blue,
        float size, float sizeJitter,
        float lifetime, float lifetimeJitter,
        float speed, float speedSpread,
        float gravity,
        float turbulence,
        float emissive,
        float density,
        float trailLength
) {

    /**
     * Weighted average of several profiles — the "slider" that blends the
     * visuals of every resolved phenomenon into one. Weights are normalised;
     * if they sum to zero we fall back to an even average so we never divide by
     * zero.
     */
    public static ParticleProfile blend(List<Weighted<ParticleProfile>> inputs) {
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Cannot blend an empty profile list");
        }

        float totalWeight = 0f;
        for (Weighted<ParticleProfile> w : inputs) {
            totalWeight += Math.max(0f, w.weight());
        }
        if (totalWeight <= 0f) {
            return average(inputs);
        }

        Accumulator acc = new Accumulator();
        for (Weighted<ParticleProfile> weighted : inputs) {
            acc.add(weighted.value(), Math.max(0f, weighted.weight()) / totalWeight);
        }
        return acc.build();
    }

    private static ParticleProfile average(List<Weighted<ParticleProfile>> inputs) {
        Accumulator acc = new Accumulator();
        float share = 1f / inputs.size();
        for (Weighted<ParticleProfile> weighted : inputs) {
            acc.add(weighted.value(), share);
        }
        return acc.build();
    }

    /** Sums profile fields, each pre-multiplied by a normalised weight. */
    private static final class Accumulator {
        float r, g, b, size, sizeJit, life, lifeJit, speed, speedSpread, gravity, turbulence, emissive, density, trail;

        void add(ParticleProfile p, float w) {
            r += p.red * w;               g += p.green * w;             b += p.blue * w;
            size += p.size * w;           sizeJit += p.sizeJitter * w;
            life += p.lifetime * w;       lifeJit += p.lifetimeJitter * w;
            speed += p.speed * w;         speedSpread += p.speedSpread * w;
            gravity += p.gravity * w;     turbulence += p.turbulence * w;
            emissive += p.emissive * w;   density += p.density * w;
            trail += p.trailLength * w;
        }

        ParticleProfile build() {
            return new ParticleProfile(r, g, b, size, sizeJit, life, lifeJit,
                    speed, speedSpread, gravity, turbulence, emissive, density, trail);
        }
    }
}