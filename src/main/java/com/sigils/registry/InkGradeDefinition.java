package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import com.sigils.core.draft.InkGrade;

/**
 * Datapack form of an ink grade.
 *
 * <pre>
 * data/&lt;pack&gt;/sigils/ink_grade/magical.json
 * { "item": "sigils:magical_ink", "units_per_item": 4.0, "tint": "#2A2440" }
 * </pre>
 */
public record InkGradeDefinition(
        Identifier item,
        float unitsPerItem,
        boolean permanent,
        int tint
) implements ItemBoundTable.Bound {
    /** Accepts {@code "#RRGGBB"} or {@code "RRGGBB"}, as elements do. */
    public static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            raw -> {
                String hex = raw.startsWith("#") ? raw.substring(1) : raw;
                try {
                    return DataResult.success(Integer.parseInt(hex, 16));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex colour: '" + raw + "' (expected #RRGGBB)");
                }
            },
            value -> "#" + String.format("%06X", value));

    public static final Codec<InkGradeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("item")
                    .forGetter(InkGradeDefinition::item),
            Codec.floatRange(0.01f, 1024f).optionalFieldOf("units_per_item", 4f)
                    .forGetter(InkGradeDefinition::unitsPerItem),
            Codec.BOOL.optionalFieldOf("permanent", false)
                    .forGetter(InkGradeDefinition::permanent),
            HEX_COLOR.optionalFieldOf("tint", 0x2A2440)
                    .forGetter(InkGradeDefinition::tint)
    ).apply(instance, InkGradeDefinition::new));

    public InkGrade toCore(Identifier id) {
        return new InkGrade(id.toString(), unitsPerItem, permanent, tint);
    }
}