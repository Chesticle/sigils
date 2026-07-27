package com.sigils.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

import com.sigils.Sigils;
import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.StrokeQuantizer;
import com.sigils.core.glyph.GlyphInstance;

/**
 * A finished draft, on its way to the server: what was placed, and the raw
 * points the player drew.
 *
 * <p>There is no fidelity field, and there is nowhere to put one. The server
 * scores the samples itself.
 */
public record SpellDraftPayload(
        int containerId,
        List<PlacedGlyph> placements,
        List<byte[]> traces
) implements CustomPacketPayload {

    public static final Type<SpellDraftPayload> TYPE = new Type<>(Sigils.id("spell_draft"));

    /** One placement, flattened for the wire. */
    public record PlacedGlyph(String glyphId, float x, float y, float rotation, float scale) {

        public static final StreamCodec<ByteBuf, PlacedGlyph> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(128), PlacedGlyph::glyphId,
                ByteBufCodecs.FLOAT, PlacedGlyph::x,
                ByteBufCodecs.FLOAT, PlacedGlyph::y,
                ByteBufCodecs.FLOAT, PlacedGlyph::rotation,
                ByteBufCodecs.FLOAT, PlacedGlyph::scale,
                PlacedGlyph::new);
    }

    private static final StreamCodec<ByteBuf, List<PlacedGlyph>> PLACEMENTS =
            PlacedGlyph.STREAM_CODEC.apply(ByteBufCodecs.list(DraftLimits.HARD_MAX_GLYPHS));

    private static final StreamCodec<ByteBuf, List<byte[]>> TRACES =
            ByteBufCodecs.byteArray(StrokeQuantizer.MAX_POINTS_PER_STROKE * 2)
                    .apply(ByteBufCodecs.list(DraftLimits.HARD_MAX_GLYPHS));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellDraftPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SpellDraftPayload::containerId,
                    PLACEMENTS, SpellDraftPayload::placements,
                    TRACES, SpellDraftPayload::traces,
                    SpellDraftPayload::new);

    /** Flatten placements for sending. */
    public static List<PlacedGlyph> from(List<GlyphInstance> placements) {
        List<PlacedGlyph> out = new ArrayList<>(placements.size());
        for (GlyphInstance placement : placements) {
            out.add(new PlacedGlyph(placement.glyphId(),
                    placement.position().x(), placement.position().y(),
                    placement.rotation(), placement.scale()));
        }
        return out;
    }

    @Override
    public Type<SpellDraftPayload> type() {
        return TYPE;
    }
}