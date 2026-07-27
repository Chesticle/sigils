package com.sigils.core.draft;

import com.sigils.core.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs a trace into two bytes per point and back again.
 *
 * <p>Canvas coordinates are 0..1, so one byte per axis gives a resolution of
 * 1/255 — about ten times finer than the tightest glyph tolerance band. The
 * precision loss is therefore invisible to scoring, and the payload shrinks
 * fourfold.
 *
 * <p>{@link #MAX_POINTS_PER_STROKE} is a <b>security limit</b>, not a style
 * choice. The client sampler stops at it, the packet codec rejects past it, and
 * the server checks it again before allocating anything.
 */
public final class StrokeQuantizer {

    /** Hard cap on sampled points in one stroke. */
    public static final int MAX_POINTS_PER_STROKE = 256;

    private StrokeQuantizer() {}

    public static byte[] encode(List<Vec2> points) {
        if (points.size() > MAX_POINTS_PER_STROKE) {
            throw new IllegalArgumentException(
                    "Stroke has " + points.size() + " points, limit is " + MAX_POINTS_PER_STROKE);
        }
        byte[] out = new byte[points.size() * 2];
        int i = 0;
        for (Vec2 p : points) {
            out[i++] = quantize(p.x());
            out[i++] = quantize(p.y());
        }
        return out;
    }

    public static List<Vec2> decode(byte[] data) {
        if (data.length % 2 != 0) {
            throw new IllegalArgumentException("Quantised stroke length must be even, got " + data.length);
        }
        if (data.length / 2 > MAX_POINTS_PER_STROKE) {
            throw new IllegalArgumentException(
                    "Quantised stroke carries " + (data.length / 2) + " points, limit is " + MAX_POINTS_PER_STROKE);
        }
        List<Vec2> points = new ArrayList<>(data.length / 2);
        for (int i = 0; i < data.length; i += 2) {
            points.add(new Vec2(dequantize(data[i]), dequantize(data[i + 1])));
        }
        return List.copyOf(points);
    }

    private static byte quantize(float v) {
        return (byte) Math.round(Math.clamp(v, 0f, 1f) * 255f);
    }

    private static float dequantize(byte b) {
        return (b & 0xFF) / 255f;
    }
}