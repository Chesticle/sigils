package com.sigils.core.particle;

/**
 * Resolves a phenomenon id or element id to the {@link ParticleProfile} that
 * draws it. The core doesn't know where profiles come from — the Minecraft
 * layer supplies this from datapack registries. Returns {@code null} for an id
 * that simply has no visual yet; the blend skips it.
 */
@FunctionalInterface
public interface ProfileLookup {
    ParticleProfile forId(String id);
}