package com.sigils.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.sigils.Sigils;
import com.sigils.core.knowledge.KnownGlyphs;
import com.sigils.registry.KnownGlyphsCodecs;

/**
 * "Here is everything you may draw."
 *
 * <p>The whole set, every time, rather than a delta. It's a few hundred bytes at
 * the very worst, it's sent on join and on unlock and never in a loop, and a
 * client that has just re-logged, changed dimension or missed a packet is
 * correct rather than nearly correct. Deltas are for things that are big or
 * frequent, and this is neither.
 */
public record KnowledgePayload(KnownGlyphs known) implements CustomPacketPayload {

    public static final Type<KnowledgePayload> TYPE = new Type<>(Sigils.id("knowledge"));

    public static final StreamCodec<ByteBuf, KnowledgePayload> STREAM_CODEC =
            KnownGlyphsCodecs.STREAM_CODEC.map(KnowledgePayload::new, KnowledgePayload::known);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}