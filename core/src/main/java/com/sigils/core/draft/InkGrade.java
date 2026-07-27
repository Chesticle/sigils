package com.sigils.core.draft;

import java.util.Objects;

/**
 * What one kind of ink is worth.
 *
 * @param id           the grade's registry id, for messages and tooltips
 * @param unitsPerItem how much {@link InkCost} budget one item provides
 * @param permanent    resists washing (Phase 6) and may be bound into a
 *                     sketchbook (Part D) — the netherite-ink gate, expressed
 *                     as a property rather than as an item name
 * @param tint         RGB the ink bar and traced lines draw in, no alpha
 */
public record InkGrade(String id, float unitsPerItem, boolean permanent, int tint) {

    public InkGrade {
        Objects.requireNonNull(id, "id");
        unitsPerItem = Math.max(0f, unitsPerItem);
    }

    /** How many whole items cover {@code cost}. */
    public int itemsFor(float cost) {
        if (cost <= 0f || unitsPerItem <= 0f) {
            return 0;
        }
        return (int) Math.ceil(cost / unitsPerItem);
    }
}