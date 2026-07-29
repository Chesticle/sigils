package com.sigils.circuit;

/**
 * Closed for the second half of every {@code period} ticks — a square wave.
 *
 * <p>A momentary pulse would be cheaper to describe and impossible to observe: a
 * trigger polled every four ticks that closes for two of every eighty is missed
 * three times in four. Half the period is unmissable at any sane poll interval,
 * and the {@link com.sigils.core.sigil.CircuitLatch} turns the long closure back
 * into a single firing.
 *
 * <p>The phase is taken from the sigil's own position, so two timers placed side
 * by side do not fire in lockstep unless you place them a whole period apart.
 */
public final class TimerCompletion implements CircuitCompletion {

    private final int period;

    public TimerCompletion(int period) {
        this.period = Math.max(4, period);
    }

    @Override
    public boolean isClosed(CircuitSite site) {
        // Two sigils side by side should not fire in lockstep, so the phase comes
        // from the position — EXCEPT inside a structure, where every cell must
        // agree. A hundred cells each on their own phase means some cell is closed
        // at almost every moment, the ring never opens, and the latch never sees a
        // rising edge: a circle on a timer that silently never fires.
        long phase = site.radius() > 0
                ? Math.floorMod(site.gameTime(), period)
                : Math.floorMod(site.gameTime() + site.origin().hashCode(), period);
        return phase >= period / 2;
    }

    @Override
    public int pollInterval() {
        return 4;
    }

    public int period() {
        return period;
    }
}