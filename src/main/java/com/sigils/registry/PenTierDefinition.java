package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.PenCapabilities;

/**
 * Datapack form of a pen tier.
 *
 * <p>The tier is keyed by its own file name and <em>binds</em> an item, rather
 * than being keyed by the item — so two items can share a tier, and a tier can
 * be renamed without renaming an item.
 *
 * <pre>
 * data/&lt;pack&gt;/sigils/pen_tier/feather.json
 * { "item": "sigils:pen", "max_complexity": 2, "instability_floor": 0.12 }
 * </pre>
 */
public record PenTierDefinition(
        Identifier item,
        int maxGlyphs,
        int maxCrests,
        int maxComplexity,
        float canvasRadius,
        boolean allowMultipleRings,
        float instabilityFactor,
        float instabilityFloor,
        boolean inklessOnSolids,
        int maxWorldSigilRadius,
        int maxArtifactTier,
        List<String> unlocks
) {
    public static final Codec<PenTierDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("item")
                    .forGetter(PenTierDefinition::item),
            Codec.intRange(1, DraftLimits.HARD_MAX_GLYPHS).optionalFieldOf("max_glyphs", 8)
                    .forGetter(PenTierDefinition::maxGlyphs),
            Codec.intRange(1, 8).optionalFieldOf("max_crests", 1)
                    .forGetter(PenTierDefinition::maxCrests),
            Codec.intRange(0, 99).optionalFieldOf("max_complexity", 2)
                    .forGetter(PenTierDefinition::maxComplexity),
            Codec.floatRange(0.05f, 0.5f).optionalFieldOf("canvas_radius", 0.45f)
                    .forGetter(PenTierDefinition::canvasRadius),
            Codec.BOOL.optionalFieldOf("allow_multiple_rings", false)
                    .forGetter(PenTierDefinition::allowMultipleRings),
            Codec.floatRange(0f, 8f).optionalFieldOf("instability_factor", 1f)
                    .forGetter(PenTierDefinition::instabilityFactor),
            Codec.floatRange(0f, 1f).optionalFieldOf("instability_floor", 0f)
                    .forGetter(PenTierDefinition::instabilityFloor),
            Codec.BOOL.optionalFieldOf("inkless_on_solids", false)
                    .forGetter(PenTierDefinition::inklessOnSolids),
            Codec.intRange(0, 64).optionalFieldOf("max_world_sigil_radius", 0)
                    .forGetter(PenTierDefinition::maxWorldSigilRadius),
            Codec.intRange(0, 8).optionalFieldOf("max_artifact_tier", 0)
                    .forGetter(PenTierDefinition::maxArtifactTier),
            Codec.STRING.listOf().optionalFieldOf("unlocks", List.of())
                    .forGetter(PenTierDefinition::unlocks)
    ).apply(instance, PenTierDefinition::new));

    /** Build the pure-core capabilities this describes. */
    public PenCapabilities toCore() {
        return new PenCapabilities(
                new DraftLimits(maxGlyphs, maxCrests, maxComplexity, canvasRadius, allowMultipleRings),
                instabilityFactor,
                instabilityFloor,
                inklessOnSolids,
                maxWorldSigilRadius,
                maxArtifactTier,
                Set.copyOf(unlocks));
    }
}