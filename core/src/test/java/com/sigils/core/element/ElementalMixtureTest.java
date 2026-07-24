package com.sigils.core.element;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ElementalMixtureTest {

    private static final String FIRE = "sigils:fire";
    private static final String WATER = "sigils:water";
    private static final String EARTH = "sigils:earth";

    @Test
    @DisplayName("a single-element mixture reports its amount and total")
    void singleElement() {
        ElementalMixture m = ElementalMixture.of(FIRE, 2.0f);
        assertEquals(2.0f, m.amountOf(FIRE), 1e-4);
        assertEquals(2.0f, m.total(), 1e-4);
        assertEquals(0.0f, m.amountOf(WATER), 1e-4);
        assertFalse(m.isEmpty());
    }

    @Test
    @DisplayName("negligible amounts are pruned to EMPTY")
    void negligibleIsEmpty() {
        assertTrue(ElementalMixture.of(FIRE, 0.0f).isEmpty());
        assertTrue(ElementalMixture.of(FIRE, 1e-9f).isEmpty());
        assertSame(ElementalMixture.EMPTY, ElementalMixture.of(FIRE, -5f));
    }

    @Test
    @DisplayName("plus merges shared elements and unions distinct ones")
    void plusMerges() {
        ElementalMixture a = ElementalMixture.of(FIRE, 1.0f);
        ElementalMixture b = ElementalMixture.of(FIRE, 0.5f).plus(ElementalMixture.of(WATER, 2.0f));
        ElementalMixture sum = a.plus(b);

        assertEquals(1.5f, sum.amountOf(FIRE), 1e-4);
        assertEquals(2.0f, sum.amountOf(WATER), 1e-4);
        assertEquals(3.5f, sum.total(), 1e-4);
    }

    @Test
    @DisplayName("equal fire and water fully consume each other, leaving no residual")
    void balancedReactionLeavesNothing() {
        // This is the steam case: 1 part fire, 1 part water.
        // The reaction consumes 1.0 of each. Nothing should survive.
        ElementalMixture pool = ElementalMixture.of(FIRE, 1.0f).plus(ElementalMixture.of(WATER, 1.0f));
        ElementalMixture consumed = ElementalMixture.of(FIRE, 1.0f).plus(ElementalMixture.of(WATER, 1.0f));

        ElementalMixture residual = pool.minus(consumed);

        assertTrue(residual.isEmpty(), "expected no residual, got " + residual);
    }

    @Test
    @DisplayName("excess fire survives the reaction as residual")
    void limitingReagentLeavesResidual() {
        // 1.5 fire, 0.5 water. Water is the limiting reagent, so the steam
        // reaction can only consume 0.5 of each. 1.0 fire should remain and
        // go on to burn things.
        ElementalMixture pool = ElementalMixture.of(FIRE, 1.5f).plus(ElementalMixture.of(WATER, 0.5f));
        ElementalMixture consumed = ElementalMixture.of(FIRE, 0.5f).plus(ElementalMixture.of(WATER, 0.5f));

        ElementalMixture residual = pool.minus(consumed);

        assertEquals(1.0f, residual.amountOf(FIRE), 1e-4);
        assertEquals(0.0f, residual.amountOf(WATER), 1e-4);
        assertFalse(residual.contains(WATER), "fully consumed elements must be pruned, not kept at zero");
    }

    @Test
    @DisplayName("minus never produces negative amounts")
    void minusFloorsAtZero() {
        ElementalMixture residual = ElementalMixture.of(FIRE, 0.5f)
                .minus(ElementalMixture.of(FIRE, 10.0f));
        assertTrue(residual.isEmpty());
    }

    @Test
    @DisplayName("ratioOf reports proportional share")
    void ratios() {
        ElementalMixture m = ElementalMixture.of(FIRE, 3.0f).plus(ElementalMixture.of(WATER, 1.0f));
        assertEquals(0.75f, m.ratioOf(FIRE), 1e-4);
        assertEquals(0.25f, m.ratioOf(WATER), 1e-4);
        assertEquals(0.0f, ElementalMixture.EMPTY.ratioOf(FIRE), 1e-4);
    }

    @Test
    @DisplayName("normalized preserves proportions and sums to one")
    void normalize() {
        ElementalMixture m = ElementalMixture.of(FIRE, 6.0f)
                .plus(ElementalMixture.of(WATER, 2.0f))
                .normalized();

        assertEquals(1.0f, m.total(), 1e-4);
        assertEquals(0.75f, m.amountOf(FIRE), 1e-4);
        assertEquals(0.25f, m.amountOf(WATER), 1e-4);
    }

    @Test
    @DisplayName("scaling by zero or less yields EMPTY")
    void scaleToNothing() {
        assertTrue(ElementalMixture.of(FIRE, 5f).scaled(0f).isEmpty());
        assertTrue(ElementalMixture.of(FIRE, 5f).scaled(-2f).isEmpty());
    }

    @Test
    @DisplayName("iteration order is deterministic regardless of insertion order")
    void deterministicOrdering() {
        // Phase 1 depends on this: reaction rules must resolve identically no
        // matter what order the crests or rule files happened to load in.
        Map<String, Float> forwards = new LinkedHashMap<>();
        forwards.put(EARTH, 1f);
        forwards.put(FIRE, 1f);
        forwards.put(WATER, 1f);

        Map<String, Float> backwards = new LinkedHashMap<>();
        backwards.put(WATER, 1f);
        backwards.put(FIRE, 1f);
        backwards.put(EARTH, 1f);

        List<String> a = new ArrayList<>(ElementalMixture.copyOf(forwards).elements());
        List<String> b = new ArrayList<>(ElementalMixture.copyOf(backwards).elements());

        assertEquals(a, b);
        assertEquals(List.of(EARTH, FIRE, WATER), a);
    }

    @Test
    @DisplayName("mixtures with the same contents are equal")
    void valueEquality() {
        ElementalMixture a = ElementalMixture.of(FIRE, 1f).plus(ElementalMixture.of(WATER, 2f));
        ElementalMixture b = ElementalMixture.of(WATER, 2f).plus(ElementalMixture.of(FIRE, 1f));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}