package com.sigils.core.cast;

/**
 * The safety limits on casting. Pure logic — no Minecraft — so the rules that
 * stop a chain spell from locking the server are unit-tested at core speed.
 *
 * <ul>
 *   <li><b>depth</b> — how deep spells-casting-spells may recurse (per chain)</li>
 *   <li><b>per-tick budget</b> — how many casts may happen server-wide per tick,
 *       a shared counter reset every tick</li>
 * </ul>
 */
public final class CastGuard {

    private final int maxDepth;
    private final int maxCastsPerTick;
    private int castsThisTick;

    public CastGuard(int maxDepth, int maxCastsPerTick) {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must be >= 0");
        }
        if (maxCastsPerTick < 1) {
            throw new IllegalArgumentException("maxCastsPerTick must be >= 1");
        }
        this.maxDepth = maxDepth;
        this.maxCastsPerTick = maxCastsPerTick;
    }

    /** Sensible defaults: 8 deep, 256 casts/tick. */
    public static CastGuard standard() {
        return new CastGuard(8, 256);
    }

    /** Consume one unit of this tick's global budget. False if the tick is full. */
    public boolean tryBeginCast() {
        if (castsThisTick >= maxCastsPerTick) {
            return false;
        }
        castsThisTick++;
        return true;
    }

    /** True if a cast at the given recursion depth is permitted. */
    public boolean withinDepth(int depth) {
        return depth >= 0 && depth <= maxDepth;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public int castsThisTick() {
        return castsThisTick;
    }

    /** Reset the per-tick budget. Call once per server tick. */
    public void resetTick() {
        castsThisTick = 0;
    }
}