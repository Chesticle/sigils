package com.sigils.circuit;

/**
 * Closed while anything adjacent carries a redstone signal.
 *
 * <p>This is the trigger that makes the roadmap's "redstone automation falls out
 * for free" true, and it is four lines. A pressure plate, a lever, a button, a
 * comparator, an observer, a daylight sensor and a hundred blocks from other mods
 * all work through it without being named.
 */
public final class RedstoneCompletion implements CircuitCompletion {

    @Override
    public boolean isClosed(CircuitSite site) {
        return site.level().hasNeighborSignal(site.origin());
    }

    @Override
    public int pollInterval() {
        return 0; // a signal change is a neighbour update; Part B listens for one
    }
}