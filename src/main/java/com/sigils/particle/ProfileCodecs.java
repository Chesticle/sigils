package com.sigils.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.sigils.core.particle.ParticleProfile;

/**
 * Mojang + network codecs for the pure-core {@link ParticleProfile}. The field
 * order in both must match the record's canonical constructor exactly.
 */
public final class ProfileCodecs {

    private ProfileCodecs() {}

    public static final MapCodec<ParticleProfile> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("red").forGetter(ParticleProfile::red),
            Codec.FLOAT.fieldOf("green").forGetter(ParticleProfile::green),
            Codec.FLOAT.fieldOf("blue").forGetter(ParticleProfile::blue),
            Codec.FLOAT.fieldOf("size").forGetter(ParticleProfile::size),
            Codec.FLOAT.fieldOf("size_jitter").forGetter(ParticleProfile::sizeJitter),
            Codec.FLOAT.fieldOf("lifetime").forGetter(ParticleProfile::lifetime),
            Codec.FLOAT.fieldOf("lifetime_jitter").forGetter(ParticleProfile::lifetimeJitter),
            Codec.FLOAT.fieldOf("speed").forGetter(ParticleProfile::speed),
            Codec.FLOAT.fieldOf("speed_spread").forGetter(ParticleProfile::speedSpread),
            Codec.FLOAT.fieldOf("gravity").forGetter(ParticleProfile::gravity),
            Codec.FLOAT.fieldOf("turbulence").forGetter(ParticleProfile::turbulence),
            Codec.FLOAT.fieldOf("emissive").forGetter(ParticleProfile::emissive),
            Codec.FLOAT.fieldOf("density").forGetter(ParticleProfile::density),
            Codec.FLOAT.fieldOf("trail").forGetter(ParticleProfile::trailLength)
    ).apply(i, ParticleProfile::new));

    /** 14 floats, in constructor order. Typed over {@link ByteBuf} so it composes
     *  into any buffer (including RegistryFriendlyByteBuf) in §4. */
    public static final StreamCodec<ByteBuf, ParticleProfile> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeFloat(p.red());   buf.writeFloat(p.green());   buf.writeFloat(p.blue());
                buf.writeFloat(p.size());  buf.writeFloat(p.sizeJitter());
                buf.writeFloat(p.lifetime());  buf.writeFloat(p.lifetimeJitter());
                buf.writeFloat(p.speed());     buf.writeFloat(p.speedSpread());
                buf.writeFloat(p.gravity());
                buf.writeFloat(p.turbulence());
                buf.writeFloat(p.emissive());
                buf.writeFloat(p.density());
                buf.writeFloat(p.trailLength());
            },
            buf -> new ParticleProfile(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat())
    );
}