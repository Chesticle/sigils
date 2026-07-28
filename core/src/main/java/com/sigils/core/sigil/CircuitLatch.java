package com.sigils.core.sigil;

/**
 * Turns a stream of "is the circuit closed?" observations into "fire now"
 * decisions.
 *
 * <p>Mutable, like {@link com.sigils.core.cast.CastGuard} and for the same
 * reason: one instance belongs to one sigil for the sigil's whole life, and
 * allocating a replacement on every observation to keep a record pure would be
 * ceremony with a cost.
 *
 * <p><b>The cooldown is stored as a deadline, not a countdown.</b> That is the
 * single most important line in this class. A countdown has to be decremented,
 * which means every placed sigil in the world needs a ticker, which is precisely
 * the trap the roadmap warns about for this phase. A deadline compared against
 * {@code level.getGameTime()} needs nothing at all to happen while it waits.
 */
public final class CircuitLatch {

    /** One second between firings, unless a caller says otherwise. */
    public static final int DEFAULT_COOLDOWN_TICKS = 20;

    private boolean closed;
    private long readyAt;

    /**
     * Observe the circuit and decide whether the sigil fires.
     *
     * @param nowClosed     what the {@code CircuitCompletion} just reported
     * @param gameTime      the level's current game time
     * @param cooldownTicks how long to refuse to fire after firing
     * @return true exactly once per closure, at most once per cooldown
     */
    public boolean advance(boolean nowClosed, long gameTime, int cooldownTicks) {
        // Time can move backwards: /time set, a world restored from a backup, or
        // a sigil that crossed a dimension boundary in a later phase. Without this
        // the deadline sits centuries in the future and the sigil is silently dead
        // forever — the worst possible failure, because nothing looks broken.
        if (readyAt > gameTime + Math.max(0, cooldownTicks)) {
            readyAt = gameTime;
        }

        boolean rising = nowClosed && !closed;
        closed = nowClosed;

        if (!rising || gameTime < readyAt) {
            return false;
        }
        readyAt = gameTime + Math.max(0, cooldownTicks);
        return true;
    }

    /** Last observed circuit state. Saved, so a reload doesn't re-fire a held plate. */
    public boolean closed() {
        return closed;
    }

    /** Game time at which this latch will next permit a firing. Saved. */
    public long readyAt() {
        return readyAt;
    }

    /** Rebuild from saved data. */
    public void restore(boolean closed, long readyAt) {
        this.closed = closed;
        this.readyAt = readyAt;
    }
}