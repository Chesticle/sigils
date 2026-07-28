package com.sigils.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What's in a vial: which grade of ink, and how much of it.
 *
 * <p>The grade is stored as its registry id string rather than as a resolved
 * {@code InkGrade}, because the grade's numbers live in a datapack that may have
 * changed since the vial was filled. The vial remembers what it holds; the table
 * decides what that's worth.
 *
 * @param grade the {@code sigils:ink_grade} id, e.g. {@code "sigils:magical"}
 * @param units how much ink remains, in the same units {@code InkCost} uses
 */
public record InkCharge(String grade, float units) {

    public static final Codec<InkCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("grade").forGetter(InkCharge::grade),
            Codec.FLOAT.fieldOf("units").forGetter(InkCharge::units)
    ).apply(instance, InkCharge::new));

    public static final StreamCodec<ByteBuf, InkCharge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), InkCharge::grade,
            ByteBufCodecs.FLOAT, InkCharge::units,
            InkCharge::new);

    public InkCharge withUnits(float remaining) {
        return new InkCharge(grade, Math.max(0f, remaining));
    }
}