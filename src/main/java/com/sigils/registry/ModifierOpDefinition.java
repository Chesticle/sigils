package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import com.sigils.core.glyph.ModifierOp;

/**
 * Datapack form of the sealed {@link ModifierOp} family.
 *
 * <pre>
 * "operation": { "type": "shape",  "shape":  "sigils:beam" }
 * "operation": { "type": "scale",  "factor": 1.5 }
 * "operation": { "type": "target", "target": "sigils:looked_at_block" }
 * </pre>
 */
public record ModifierOpDefinition(
        String type,
        Optional<String> shape,
        Optional<Float> factor,
        Optional<String> target
) {
    private static final Codec<ModifierOpDefinition> RAW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(ModifierOpDefinition::type),
            Codec.STRING.optionalFieldOf("shape").forGetter(ModifierOpDefinition::shape),
            Codec.FLOAT.optionalFieldOf("factor").forGetter(ModifierOpDefinition::factor),
            Codec.STRING.optionalFieldOf("target").forGetter(ModifierOpDefinition::target)
    ).apply(instance, ModifierOpDefinition::new));

    /** The codec a glyph actually uses: reads JSON straight into the core type. */
    public static final Codec<ModifierOp> OP_CODEC =
            RAW.comapFlatMap(ModifierOpDefinition::toCore, ModifierOpDefinition::fromCore);

    private DataResult<ModifierOp> toCore() {
        return switch (type) {
            case "shape" -> shape
                    .<DataResult<ModifierOp>>map(s -> DataResult.success(new ModifierOp.Shape(s)))
                    .orElseGet(() -> DataResult.error(() -> "A 'shape' modifier needs a \"shape\" field"));
            case "scale" -> factor
                    .<DataResult<ModifierOp>>map(f -> DataResult.success(new ModifierOp.Scale(f)))
                    .orElseGet(() -> DataResult.error(() -> "A 'scale' modifier needs a \"factor\" field"));
            case "target" -> target
                    .<DataResult<ModifierOp>>map(t -> DataResult.success(new ModifierOp.Target(t)))
                    .orElseGet(() -> DataResult.error(() -> "A 'target' modifier needs a \"target\" field"));
            default -> DataResult.error(
                    () -> "Unknown modifier type '" + type + "' (expected shape, scale or target)");
        };
    }

    private static ModifierOpDefinition fromCore(ModifierOp op) {
        // Exhaustive over the sealed interface: add a ModifierOp kind and this
        // switch stops compiling until you handle it.
        return switch (op) {
            case ModifierOp.Shape s ->
                    new ModifierOpDefinition("shape", Optional.of(s.shapeId()), Optional.empty(), Optional.empty());
            case ModifierOp.Scale s ->
                    new ModifierOpDefinition("scale", Optional.empty(), Optional.of(s.factor()), Optional.empty());
            case ModifierOp.Target t ->
                    new ModifierOpDefinition("target", Optional.empty(), Optional.empty(), Optional.of(t.targetId()));
        };
    }
}