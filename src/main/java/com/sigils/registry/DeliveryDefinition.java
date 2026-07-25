package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.sigils.core.spell.Delivery;

/** Datapack form of a {@link Delivery}: how a spell is projected. */
public record DeliveryDefinition(String shape, float scale, int duration, String target) {

    public static final Codec<DeliveryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("shape").forGetter(DeliveryDefinition::shape),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(DeliveryDefinition::scale),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(DeliveryDefinition::duration),
            Codec.STRING.fieldOf("target").forGetter(DeliveryDefinition::target)
    ).apply(instance, DeliveryDefinition::new));

    public Delivery toCore() {
        return new Delivery(shape, scale, duration, target);
    }
}