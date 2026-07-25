package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

import com.sigils.core.SigilsCore;
import com.sigils.core.element.ElementalMixture;
import com.sigils.core.spell.CompiledSpell;

/**
 * A spell authored as data: an elemental mixture, a delivery, and a fidelity.
 *
 * <p>Because there's no drawing UI yet, we author the mixture directly instead
 * of deriving it from traced glyphs — fidelity is just set to 1.0. When the UI
 * arrives (Phase 4) it produces the same {@link CompiledSpell} from real traces.
 */
public record SpellDefinition(Map<String, Float> mixture, DeliveryDefinition delivery, float fidelity) {

    public static final Codec<SpellDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("mixture").forGetter(SpellDefinition::mixture),
            DeliveryDefinition.CODEC.fieldOf("delivery").forGetter(SpellDefinition::delivery),
            Codec.FLOAT.optionalFieldOf("fidelity", 1f).forGetter(SpellDefinition::fidelity)
    ).apply(instance, SpellDefinition::new));

    /** Build the pure-core {@link CompiledSpell} this describes. */
    public CompiledSpell toCompiled() {
        ElementalMixture mix = ElementalMixture.EMPTY;
        for (Map.Entry<String, Float> entry : mixture.entrySet()) {
            mix = mix.plus(ElementalMixture.of(entry.getKey(), entry.getValue()));
        }
        return new CompiledSpell(
                SigilsCore.SPELL_SCHEMA_VERSION, mix, delivery.toCore(), fidelity, List.of());
    }
}