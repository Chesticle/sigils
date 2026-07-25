package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

import com.sigils.core.reaction.Reagent;
import com.sigils.core.reaction.ReactionRule;

/**
 * Datapack description of a reaction. {@link #toCore(Identifier)} converts it to
 * the pure-core {@link ReactionRule} the resolver consumes — the boundary
 * between "Minecraft data" and "engine".
 */
public record ReactionRuleDefinition(
        List<ReagentDefinition> inputs,
        String output,
        float yield,
        int priority
) {
    public static final Codec<ReactionRuleDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReagentDefinition.CODEC.listOf().fieldOf("inputs").forGetter(ReactionRuleDefinition::inputs),
            Codec.STRING.fieldOf("output").forGetter(ReactionRuleDefinition::output),
            Codec.FLOAT.optionalFieldOf("yield", 1f).forGetter(ReactionRuleDefinition::yield),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ReactionRuleDefinition::priority)
    ).apply(instance, ReactionRuleDefinition::new));

    public ReactionRule toCore(Identifier id) {
        List<Reagent> coreInputs = inputs.stream().map(ReagentDefinition::toCore).toList();
        return new ReactionRule(id.toString(), coreInputs, output, yield, priority);
    }

    /** One reagent line in a reaction JSON. */
    public record ReagentDefinition(String element, float minRatio, float maxRatio, float consumption) {

        public static final Codec<ReagentDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("element").forGetter(ReagentDefinition::element),
                Codec.FLOAT.optionalFieldOf("min_ratio", 0f).forGetter(ReagentDefinition::minRatio),
                Codec.FLOAT.optionalFieldOf("max_ratio", 1f).forGetter(ReagentDefinition::maxRatio),
                Codec.FLOAT.fieldOf("consumption").forGetter(ReagentDefinition::consumption)
        ).apply(instance, ReagentDefinition::new));

        public Reagent toCore() {
            return new Reagent(element, minRatio, maxRatio, consumption);
        }
    }
}