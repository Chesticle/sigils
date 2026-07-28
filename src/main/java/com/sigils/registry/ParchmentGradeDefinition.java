package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * Datapack form of a parchment grade — how faithfully a sheet holds a traced
 * line.
 *
 * <p>{@code quality} multiplies the fidelity a trace earns: 1.0 is neutral,
 * above 1 forgives a slightly shaky hand, below 1 blurs a steady one. It is fed
 * to {@link com.sigils.core.draft.DraftQuality} at inscribe time.
 *
 * <pre>
 * data/&lt;pack&gt;/sigils/parchment_grade/vellum.json
 * { "item": "sigils:vellum", "quality": 1.08 }
 * </pre>
 */
public record ParchmentGradeDefinition(
        Identifier item,
        float quality
) implements ItemBoundTable.Bound {

    public static final Codec<ParchmentGradeDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("item")
                            .forGetter(ParchmentGradeDefinition::item),
                    Codec.floatRange(0.1f, 1.5f).optionalFieldOf("quality", 1f)
                            .forGetter(ParchmentGradeDefinition::quality)
            ).apply(instance, ParchmentGradeDefinition::new));
}