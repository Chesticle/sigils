package com.sigils.core.util;

/** A value paired with a blend weight. */
public record Weighted<T>(T value, float weight) {
    public static <T> Weighted<T> of(T value, float weight) {
        return new Weighted<>(value, weight);
    }
}