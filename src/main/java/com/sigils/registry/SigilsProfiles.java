package com.sigils.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import com.sigils.core.element.Element;
import com.sigils.core.particle.ElementVisuals;
import com.sigils.core.particle.ProfileLookup;

/**
 * Bridges the datapack registries to the pure-core {@link ProfileLookup}.
 *
 * <p>Resolution order for an id:
 * <ol>
 *   <li>an explicit phenomenon preset in {@code sigils:particle_profile}</li>
 *   <li>an element, whose profile is DERIVED from its own fields (no rendering
 *       data — the Phase 3 extensibility contract)</li>
 *   <li>nothing → {@code null} (the id has no visual yet; the blend skips it)</li>
 * </ol>
 */
public final class SigilsProfiles {

    private SigilsProfiles() {}

    public static ProfileLookup lookup(RegistryAccess access) {
        Registry<ParticleProfileDefinition> presets = access.lookupOrThrow(SigilsRegistries.PARTICLE_PROFILE);
        Registry<ElementDefinition> elements = access.lookupOrThrow(SigilsRegistries.ELEMENT);

        return id -> {
            Identifier key = Identifier.tryParse(id);
            if (key == null) {
                return null;
            }
            ParticleProfileDefinition preset = presets.getValue(key);
            if (preset != null) {
                return preset.toCore();
            }
            ElementDefinition element = elements.getValue(key);
            if (element != null) {
                Element core = element.toCore(key);
                return ElementVisuals.profileFor(core);
            }
            return null;
        };
    }
}