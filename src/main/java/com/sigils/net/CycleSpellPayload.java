package com.sigils.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.sigils.Sigils;

/**
 * "Turn the page." Carries a direction and nothing else.
 *
 * <p>Deliberately not "select page 4": an index would let a client claim a
 * selection the server never agreed to, and the server would have to validate it
 * anyway. A sign is trivially safe — the worst a forged packet achieves is
 * selecting a different spell the player already owns.
 */
public record CycleSpellPayload(int direction) implements CustomPacketPayload {

    public static final Type<CycleSpellPayload> TYPE = new Type<>(Sigils.id("cycle_spell"));

    public static final StreamCodec<ByteBuf, CycleSpellPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeByte(payload.direction() >= 0 ? 1 : -1),
            buffer -> new CycleSpellPayload(buffer.readByte() >= 0 ? 1 : -1));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}