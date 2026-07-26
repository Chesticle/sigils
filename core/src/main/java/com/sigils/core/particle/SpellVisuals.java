package com.sigils.core.particle;

import com.sigils.core.reaction.Resolution;
import com.sigils.core.util.Weighted;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a {@link Resolution} into one blended {@link ParticleProfile}: every
 * phenomenon and every leftover element contributes its profile, weighted by
 * the strength the resolver gave it. This is the "one source of truth drives
 * both effect and visual" pillar as arithmetic — the same numbers the effect
 * handlers act on decide the look.
 */
public final class SpellVisuals {

    private SpellVisuals() {}

    /** Empty if nothing in the resolution has a registered visual (an inert spell). */
    public static Optional<ParticleProfile> blend(Resolution resolution, ProfileLookup lookup) {
        List<Weighted<ParticleProfile>> weighted = new ArrayList<>();
        collect(resolution.phenomena(), lookup, weighted);        // reaction phenomena
        collect(resolution.residual().asMap(), lookup, weighted); // leftover elements
        if (weighted.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ParticleProfile.blend(weighted));
    }

    private static void collect(Map<String, Float> source, ProfileLookup lookup,
                                List<Weighted<ParticleProfile>> out) {
        for (Map.Entry<String, Float> entry : source.entrySet()) {
            float weight = entry.getValue();
            if (weight <= 0f) {
                continue;
            }
            ParticleProfile profile = lookup.forId(entry.getKey());
            if (profile != null) {
                out.add(Weighted.of(profile, weight));
            }
        }
    }
}