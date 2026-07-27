package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Map;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.core.spell.Delivery;

/** Disk and network forms of a {@link CompiledSpell}, for storage on an item. */
public final class CompiledSpellCodecs {

    private CompiledSpellCodecs() {}

    public static final Codec<ElementalMixture> MIXTURE =
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                    .xmap(ElementalMixture::copyOf, mixture -> (Map<String, Float>) mixture.asMap());

    public static final Codec<Delivery> DELIVERY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("shape").forGetter(Delivery::shapeId),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Delivery::scale),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(Delivery::durationTicks),
            Codec.STRING.fieldOf("target").forGetter(Delivery::targetId)
    ).apply(instance, Delivery::new));

    public static final Codec<CompiledSpell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(CompiledSpell::schemaVersion),
            MIXTURE.fieldOf("mixture").forGetter(CompiledSpell::mixture),
            DELIVERY.fieldOf("delivery").forGetter(CompiledSpell::delivery),
            Codec.FLOAT.optionalFieldOf("fidelity", 1f).forGetter(CompiledSpell::fidelity),
            Codec.STRING.listOf().optionalFieldOf("rings", List.of()).forGetter(CompiledSpell::rings)
    ).apply(instance, CompiledSpell::new));

    /** Network form. Fine to derive from the codec: spells are sent rarely and are small. */
    public static final StreamCodec<ByteBuf, CompiledSpell> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);
}