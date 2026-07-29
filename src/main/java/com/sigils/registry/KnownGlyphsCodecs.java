package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashSet;

import com.sigils.core.knowledge.KnownGlyphs;

/**
 * Disk and wire forms for {@link KnownGlyphs}.
 *
 * <p>A plain list of strings, written sorted. Nothing clever: the whole value of
 * this format is that a server owner can open a player's data file, read what
 * they know, and edit it with a text editor when something goes wrong.
 *
 * <p><b>Declaration order in this file is load-bearing.</b> {@code MAP_CODEC} is
 * derived from {@code CODEC} and must therefore appear after it — static fields
 * initialise top to bottom, and reaching a later one through a method call
 * silently yields null rather than failing to compile.
 */
public final class KnownGlyphsCodecs {

    private KnownGlyphsCodecs() {}

    /**
     * Ceiling on how many glyphs one player may know.
     *
     * <p>This is a wire-safety limit, not a game rule — it bounds what a
     * malformed or malicious packet can make the client allocate. It wants to be
     * comfortably larger than any plausible modpack's glyph count.
     */
    public static final int MAX_GLYPHS = 1024;

    public static final Codec<KnownGlyphs> CODEC = Codec.STRING.listOf().xmap(
            list -> new KnownGlyphs(new LinkedHashSet<>(list)),
            KnownGlyphs::sorted);

    /**
     * The attachment form. {@code AttachmentType.Builder#serialize} takes a
     * {@link MapCodec} because it writes into a compound tag it doesn't own, so
     * the value needs a field name to live under rather than being the whole
     * document.
     *
     * <p>On disk that's {@code {glyphs: ["sigils:mod_beam", ...]}} — which is
     * also the better shape long-term: a schema version or a per-glyph unlock
     * timestamp becomes a sibling field rather than a format break.
     */
    public static final MapCodec<KnownGlyphs> MAP_CODEC = CODEC.fieldOf("glyphs");

    public static final StreamCodec<ByteBuf, KnownGlyphs> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(256)
                    .apply(ByteBufCodecs.list(MAX_GLYPHS))
                    .map(list -> new KnownGlyphs(new LinkedHashSet<>(list)),
                            KnownGlyphs::sorted);
}