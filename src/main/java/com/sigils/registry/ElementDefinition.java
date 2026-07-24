package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sigils.core.element.Element;
import net.minecraft.resources.Identifier;

/**
 * The JSON-facing form of an {@link Element}.
 *
 * <p>The pure-Java {@code core} module has no serialization library, so the
 * codec lives here and translates across the boundary. That keeps the physics
 * portable and keeps Minecraft types out of the engine.
 *
 * <p>Elements are loaded from datapack JSON at world load, which means adding a
 * fifth element is a file, not a commit. That is the whole thesis of the mod,
 * and Phase 0 exists to prove it works before anything is built on top.
 */
public record ElementDefinition(
        int colorLinear,
        float density,
        float volatility,
        float luminance
) {

    /** Accepts {@code "#RRGGBB"} or {@code "RRGGBB"} so JSON stays human-readable. */
    private static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            raw -> {
                String hex = raw.startsWith("#") ? raw.substring(1) : raw;
                try {
                    return DataResult.success(Integer.parseInt(hex, 16));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex colour: '" + raw + "' (expected #RRGGBB)");
                }
            },
            value -> "#" + String.format("%06X", value)
    );

    public static final Codec<ElementDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    HEX_COLOR.fieldOf("color")
                            .forGetter(ElementDefinition::colorLinear),
                    Codec.FLOAT.optionalFieldOf("density", 0.0f)
                            .forGetter(ElementDefinition::density),
                    Codec.FLOAT.optionalFieldOf("volatility", 0.5f)
                            .forGetter(ElementDefinition::volatility),
                    Codec.FLOAT.optionalFieldOf("luminance", 0.0f)
                            .forGetter(ElementDefinition::luminance)
            ).apply(instance, ElementDefinition::new)
    );

    /** Converts to the engine's representation. */
    public Element toCore(Identifier id) {
        return new Element(id.toString(), colorLinear, density, volatility, luminance);
    }
}