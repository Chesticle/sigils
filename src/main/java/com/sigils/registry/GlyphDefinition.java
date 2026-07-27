package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.ModifierOp;

/**
 * Datapack form of a {@link Glyph}: the strokes a player must trace, how tightly,
 * what it costs, and what it does.
 *
 * <p>Strokes are authored in the glyph's own 0..1 box with y growing downward,
 * so (0.5, 0.1) is near the top. The canvas transforms them by the placement.
 */
public record GlyphDefinition(
        GlyphRole role,
        List<StrokePath> strokes,
        float tolerance,
        int complexity,
        float inkCost,
        Optional<Map<String, Float>> contribution,
        Optional<ModifierOp> operation
) {
    /** A point: a two-number array, [x, y]. */
    public static final Codec<Vec2> VEC2 = Codec.FLOAT.listOf().comapFlatMap(
            list -> list.size() == 2
                    ? DataResult.success(new Vec2(list.get(0), list.get(1)))
                    : DataResult.error(() -> "A point needs exactly two numbers, got " + list.size()),
            v -> List.of(v.x(), v.y()));

    /** A stroke: an array of at least two points. */
    public static final Codec<StrokePath> STROKE = VEC2.listOf().comapFlatMap(
            points -> points.size() >= 2
                    ? DataResult.success(new StrokePath(points))
                    : DataResult.error(() -> "A stroke needs at least two points"),
            StrokePath::points);

    public static final Codec<GlyphRole> ROLE = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(GlyphRole.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.<GlyphRole>error(
                            () -> "Unknown glyph role '" + name + "' (crest, modifier, ring or link)");
                }
            },
            role -> role.name().toLowerCase(Locale.ROOT));

    public static final Codec<GlyphDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ROLE.fieldOf("role").forGetter(GlyphDefinition::role),
            STROKE.listOf().fieldOf("strokes").forGetter(GlyphDefinition::strokes),
            Codec.FLOAT.optionalFieldOf("tolerance", 0.05f).forGetter(GlyphDefinition::tolerance),
            Codec.INT.optionalFieldOf("complexity", 1).forGetter(GlyphDefinition::complexity),
            Codec.FLOAT.optionalFieldOf("ink_cost", 1f).forGetter(GlyphDefinition::inkCost),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("contribution")
                    .forGetter(GlyphDefinition::contribution),
            ModifierOpDefinition.OP_CODEC.optionalFieldOf("operation")
                    .forGetter(GlyphDefinition::operation)
    ).apply(instance, GlyphDefinition::new));

    /** Build the pure-core {@link Glyph} this describes. */
    public Glyph toCore(Identifier id) {
        return new Glyph(
                id.toString(),
                role,
                strokes,
                tolerance,
                complexity,
                inkCost,
                contribution.map(ElementalMixture::copyOf),
                operation);
    }
}