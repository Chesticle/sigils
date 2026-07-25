package com.sigils.core.geometry;

/** An immutable 2D point/vector in normalised canvas space (0..1 on each axis). */
public record Vec2(float x, float y) {

    public Vec2 plus(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }

    public Vec2 minus(Vec2 o) {
        return new Vec2(x - o.x, y - o.y);
    }

    public Vec2 scaled(float f) {
        return new Vec2(x * f, y * f);
    }

    public float dot(Vec2 o) {
        return x * o.x + y * o.y;
    }

    public float lengthSquared() {
        return x * x + y * y;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float distanceTo(Vec2 o) {
        return minus(o).length();
    }

    /** Distance without the square root — cheaper when you only need to compare. */
    public float distanceSquaredTo(Vec2 o) {
        return minus(o).lengthSquared();
    }
}