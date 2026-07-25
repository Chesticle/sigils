package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Datapack description of a phenomenon. Minimal for now — a label and an
 * intensity scale. Effect handlers and particle links get added as optional
 * fields in later phases (old JSON keeps working, which is why they're optional).
 */
public record PhenomenonDefinition(String description, float intensityScale) {

    public static final Codec<PhenomenonDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("description", "").forGetter(PhenomenonDefinition::description),
            Codec.FLOAT.optionalFieldOf("intensity_scale", 1f).forGetter(PhenomenonDefinition::intensityScale)
    ).apply(instance, PhenomenonDefinition::new));
}