package com.sigils.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;

/** The mod's single particle type. Its payload — the profile — is what varies. */
public final class SigilsParticles {

    private SigilsParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Sigils.MOD_ID);

    public static final Supplier<ParticleType<SigilParticleOptions>> SIGIL =
            PARTICLE_TYPES.register("sigil", () -> new ParticleType<SigilParticleOptions>(false) {
                @Override
                public MapCodec<SigilParticleOptions> codec() {
                    return SigilParticleOptions.MAP_CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, SigilParticleOptions> streamCodec() {
                    return SigilParticleOptions.STREAM_CODEC;
                }
            });

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}