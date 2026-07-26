package com.sigils.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import com.sigils.Sigils;
import com.sigils.core.particle.ParticleProfile;
import com.sigils.particle.ProfileCodecs;

/**
 * The ONE packet Phase 3 sends per cast: everything a client needs to run an
 * emitter locally. Never per-particle — the client spawns its own particles
 * from this description.
 */
public record SigilEmitterPayload(
        ParticleProfile profile,
        String shapeId,
        Vec3 origin,
        Vec3 target,
        float scale,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<SigilEmitterPayload> TYPE = new Type<>(Sigils.id("emitter"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.of(
            (buf, v) -> { buf.writeDouble(v.x); buf.writeDouble(v.y); buf.writeDouble(v.z); },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static final StreamCodec<RegistryFriendlyByteBuf, SigilEmitterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ProfileCodecs.STREAM_CODEC, SigilEmitterPayload::profile,
                    ByteBufCodecs.STRING_UTF8,  SigilEmitterPayload::shapeId,
                    VEC3,                        SigilEmitterPayload::origin,
                    VEC3,                        SigilEmitterPayload::target,
                    ByteBufCodecs.FLOAT,         SigilEmitterPayload::scale,
                    ByteBufCodecs.VAR_INT,       SigilEmitterPayload::durationTicks,
                    SigilEmitterPayload::new);

    @Override
    public Type<SigilEmitterPayload> type() {
        return TYPE;
    }
}