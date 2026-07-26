package com.sigils.registry; // match your ElementDefinition package

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.sigils.core.particle.ParticleProfile;

/**
 * Datapack form of a {@link ParticleProfile} preset — the look a phenomenon owns.
 *
 * <p>Colour is authored in sRGB hex ({@code "#RRGGBB"}) and converted to LINEAR
 * in {@link #toCore()}, because {@link ParticleProfile#blend} must run in linear
 * space or blended colours turn to grey mud.
 */
public record ParticleProfileDefinition(
        int colorSrgb,
        float size, float sizeJitter,
        float lifetime, float lifetimeJitter,
        float speed, float speedSpread,
        float gravity,
        float turbulence,
        float emissive,
        float density,
        float trailLength
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
            value -> "#" + String.format("%06X", value));

    public static final Codec<ParticleProfileDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            HEX_COLOR.fieldOf("color").forGetter(ParticleProfileDefinition::colorSrgb),
            Codec.FLOAT.optionalFieldOf("size", 0.2f).forGetter(ParticleProfileDefinition::size),
            Codec.FLOAT.optionalFieldOf("size_jitter", 0.05f).forGetter(ParticleProfileDefinition::sizeJitter),
            Codec.FLOAT.optionalFieldOf("lifetime", 16f).forGetter(ParticleProfileDefinition::lifetime),
            Codec.FLOAT.optionalFieldOf("lifetime_jitter", 4f).forGetter(ParticleProfileDefinition::lifetimeJitter),
            Codec.FLOAT.optionalFieldOf("speed", 0.1f).forGetter(ParticleProfileDefinition::speed),
            Codec.FLOAT.optionalFieldOf("speed_spread", 0.03f).forGetter(ParticleProfileDefinition::speedSpread),
            Codec.FLOAT.optionalFieldOf("gravity", 0f).forGetter(ParticleProfileDefinition::gravity),
            Codec.FLOAT.optionalFieldOf("turbulence", 0.05f).forGetter(ParticleProfileDefinition::turbulence),
            Codec.FLOAT.optionalFieldOf("emissive", 0f).forGetter(ParticleProfileDefinition::emissive),
            Codec.FLOAT.optionalFieldOf("density", 1f).forGetter(ParticleProfileDefinition::density),
            Codec.FLOAT.optionalFieldOf("trail", 0f).forGetter(ParticleProfileDefinition::trailLength)
    ).apply(i, ParticleProfileDefinition::new));

    /** To the engine's representation, with colour linearised. */
    public ParticleProfile toCore() {
        float r = srgbToLinear(((colorSrgb >> 16) & 0xFF) / 255f);
        float g = srgbToLinear(((colorSrgb >> 8) & 0xFF) / 255f);
        float b = srgbToLinear((colorSrgb & 0xFF) / 255f);
        return new ParticleProfile(r, g, b, size, sizeJitter, lifetime, lifetimeJitter,
                speed, speedSpread, gravity, turbulence, emissive, density, trailLength);
    }

    /** sRGB 0..1 → linear 0..1 (IEC 61966-2-1). */
    public static float srgbToLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }
}