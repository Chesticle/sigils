package com.sigils.core.spell;

import java.util.Objects;

/**
 * How a spell is delivered: its form, size, how long it persists, and what it
 * aims at. Shape and target are data ids resolved to behaviour in the Minecraft
 * layer (Phase 2) — the core never hardcodes what "beam" means.
 */
public record Delivery(String shapeId, float scale, int durationTicks, String targetId) {
    public Delivery {
        Objects.requireNonNull(shapeId, "shapeId");
        Objects.requireNonNull(targetId, "targetId");
    }
}