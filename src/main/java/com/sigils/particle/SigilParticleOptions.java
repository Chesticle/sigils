package com.sigils.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.sigils.core.particle.ParticleProfile;

/** A {@link ParticleOptions} carrying a blended {@link ParticleProfile}. */
public record SigilParticleOptions(ParticleProfile profile) implements ParticleOptions {

    public static final MapCodec<SigilParticleOptions> MAP_CODEC =
            ProfileCodecs.MAP_CODEC.xmap(SigilParticleOptions::new, SigilParticleOptions::profile);

    public static final StreamCodec<? super RegistryFriendlyByteBuf, SigilParticleOptions> STREAM_CODEC =
            ProfileCodecs.STREAM_CODEC.map(SigilParticleOptions::new, SigilParticleOptions::profile);

    @Override
    public ParticleType<SigilParticleOptions> getType() {
        return SigilsParticles.SIGIL.get();
    }
}