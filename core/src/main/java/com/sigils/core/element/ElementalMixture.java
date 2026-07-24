package com.sigils.core.element;

import com.sigils.core.SigilsCore;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An immutable bag of element amounts — the currency of the entire spell engine.
 *
 * <p>A crest contributes a mixture. Modifiers scale it. Reactions consume from
 * it and leave a residual. That residual is what makes the system feel physical
 * instead of like a lookup table:
 *
 * <pre>
 *   fire 1.0 + water 1.0  -&gt; steam reaction consumes both -&gt; residual EMPTY
 *                            (pure bubbles, nothing else)
 *
 *   fire 1.5 + water 0.5  -&gt; water is the limiting reagent, so the reaction
 *                            consumes 0.5 of each -&gt; residual fire 1.0
 *                            (fewer bubbles, plus real flame)
 * </pre>
 *
 * <p>Invariants, upheld by every factory and operation:
 * <ul>
 *   <li>No stored amount is ever {@code <= EPSILON} — tiny values are pruned.</li>
 *   <li>Iteration order is sorted by element id, so results are deterministic
 *       regardless of the order rules or crests were processed in.</li>
 *   <li>The instance is deeply immutable.</li>
 * </ul>
 */
public final class ElementalMixture {

    public static final ElementalMixture EMPTY = new ElementalMixture(Collections.emptySortedMap());

    private final SortedMap<String, Float> parts;

    private ElementalMixture(SortedMap<String, Float> parts) {
        this.parts = parts;
    }

    // ---------------------------------------------------------------- factories

    /** A mixture of a single element. Returns {@link #EMPTY} for negligible amounts. */
    public static ElementalMixture of(String elementId, float amount) {
        if (amount <= SigilsCore.EPSILON) {
            return EMPTY;
        }
        SortedMap<String, Float> map = new TreeMap<>();
        map.put(elementId, amount);
        return new ElementalMixture(Collections.unmodifiableSortedMap(map));
    }

    /** Builds a mixture from arbitrary input, pruning negligible and negative amounts. */
    public static ElementalMixture copyOf(Map<String, Float> raw) {
        SortedMap<String, Float> map = new TreeMap<>();
        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            Float value = entry.getValue();
            if (value != null && value > SigilsCore.EPSILON) {
                map.put(entry.getKey(), value);
            }
        }
        return map.isEmpty() ? EMPTY : new ElementalMixture(Collections.unmodifiableSortedMap(map));
    }

    // ---------------------------------------------------------------- queries

    /** Unmodifiable, id-sorted view of the contents. */
    public SortedMap<String, Float> asMap() {
        return parts;
    }

    public Set<String> elements() {
        return parts.keySet();
    }

    public float amountOf(String elementId) {
        return parts.getOrDefault(elementId, 0f);
    }

    public float total() {
        float sum = 0f;
        for (float value : parts.values()) {
            sum += value;
        }
        return sum;
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public boolean contains(String elementId) {
        return parts.containsKey(elementId);
    }

    /** This element's share of the whole, 0..1. Returns 0 for an empty mixture. */
    public float ratioOf(String elementId) {
        float total = total();
        return total <= SigilsCore.EPSILON ? 0f : amountOf(elementId) / total;
    }

    // ---------------------------------------------------------------- arithmetic

    public ElementalMixture plus(ElementalMixture other) {
        if (other.isEmpty()) return this;
        if (this.isEmpty()) return other;

        SortedMap<String, Float> merged = new TreeMap<>(this.parts);
        for (Map.Entry<String, Float> entry : other.parts.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Float::sum);
        }
        return copyOf(merged);
    }

    /** Subtracts, flooring each element at zero. This is how reactions consume reagents. */
    public ElementalMixture minus(ElementalMixture other) {
        if (other.isEmpty() || this.isEmpty()) return this;

        SortedMap<String, Float> result = new TreeMap<>(this.parts);
        for (Map.Entry<String, Float> entry : other.parts.entrySet()) {
            Float current = result.get(entry.getKey());
            if (current != null) {
                result.put(entry.getKey(), Math.max(0f, current - entry.getValue()));
            }
        }
        return copyOf(result);
    }

    public ElementalMixture scaled(float factor) {
        if (factor <= 0f || isEmpty()) return EMPTY;

        SortedMap<String, Float> result = new TreeMap<>();
        for (Map.Entry<String, Float> entry : parts.entrySet()) {
            result.put(entry.getKey(), entry.getValue() * factor);
        }
        return copyOf(result);
    }

    /** Same proportions, total of exactly 1.0. Empty stays empty. */
    public ElementalMixture normalized() {
        float total = total();
        if (total <= SigilsCore.EPSILON) return EMPTY;
        return scaled(1f / total);
    }

    // ---------------------------------------------------------------- identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ElementalMixture other && parts.equals(other.parts);
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

    @Override
    public String toString() {
        if (parts.isEmpty()) return "Mixture[empty]";
        StringBuilder sb = new StringBuilder("Mixture[");
        boolean first = true;
        for (Map.Entry<String, Float> entry : parts.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(" x").append(String.format("%.2f", entry.getValue()));
            first = false;
        }
        return sb.append(']').toString();
    }
}