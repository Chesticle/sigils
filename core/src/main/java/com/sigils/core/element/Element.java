package com.sigils.core.element;

import java.util.Objects;

/**
 * A fundamental element. Fire, water, earth, air — and whatever you add later.
 *
 * <p>Elements are <em>data</em>. This record is loaded from JSON at world load
 * (see the datapack registry in the Minecraft module). Adding a fifth element
 * must never require touching Java.
 *
 * <p>The {@code id} is a plain string like {@code "sigils:fire"} rather than a
 * Minecraft {@code Identifier}, so that this module stays free of game imports.
 * Translation happens at the boundary.
 *
 * @param id           namespaced id, e.g. {@code "sigils:fire"}
 * @param colorLinear  packed 0xRRGGBB in <em>linear</em> colour space, not sRGB.
 *                     Blending happens on these values; sRGB lerping turns
 *                     orange-plus-blue into mud.
 * @param density      &lt;0 rises, &gt;0 sinks. Drives particle gravity and
 *                     effect bias (smoke climbs, silt settles).
 * @param volatility   0..1 reaction eagerness. Also amplifies instability from
 *                     sloppy tracing.
 * @param luminance    0..1 baseline glow contribution.
 */
public record Element(
        String id,
        int colorLinear,
        float density,
        float volatility,
        float luminance
) {
    public Element {
        Objects.requireNonNull(id, "element id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Element id must not be blank");
        }
        if (volatility < 0f || volatility > 1f) {
            throw new IllegalArgumentException("volatility must be 0..1, got " + volatility);
        }
        if (luminance < 0f || luminance > 1f) {
            throw new IllegalArgumentException("luminance must be 0..1, got " + luminance);
        }
    }
}