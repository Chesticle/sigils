package com.sigils.core.glyph;

/** What a modifier glyph does to a spell's delivery. Shapes/targets are data ids. */
public sealed interface ModifierOp {

    /** Set the delivery form, e.g. {@code "sigils:beam"}. */
    record Shape(String shapeId) implements ModifierOp {}

    /** Multiply the delivery scale. */
    record Scale(float factor) implements ModifierOp {}

    /** Set the delivery target, e.g. {@code "sigils:looked_at_block"}. */
    record Target(String targetId) implements ModifierOp {}
}