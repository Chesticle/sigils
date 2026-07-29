package com.sigils.core.sigil;

/**
 * An offset within the plane a sigil is drawn on, in cells.
 *
 * <p>Deliberately not a 3D offset: a surface sigil lies flat, so its footprint is
 * two-dimensional and which two axes those are is a question for the Minecraft
 * layer. Keeping it abstract is what lets the ring maths be tested without a
 * world.
 */
public record PlaneOffset(int a, int b) {

    public static final PlaneOffset CENTRE = new PlaneOffset(0, 0);
}