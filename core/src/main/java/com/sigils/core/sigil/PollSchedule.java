package com.sigils.core.sigil;

/**
 * Whose turn it is to be looked at this tick.
 *
 * <p>Two hundred sigils that each need checking every four ticks is fifty checks
 * a tick if you spread them and two hundred checks on every fourth tick if you
 * don't. Same total work; wildly different tick-time spikes. Hashing the position
 * into the phase is the whole trick.
 */
public final class PollSchedule {

    private PollSchedule() {}

    /**
     * @param gameTime     the level's current game time
     * @param positionHash any stable integer derived from the sigil's position
     * @param intervalTicks 0 or less means "never polled — something else wakes it"
     * @return true on exactly one tick in every {@code intervalTicks}
     */
    public static boolean due(long gameTime, int positionHash, int intervalTicks) {
        if (intervalTicks <= 0) {
            return false;
        }
        if (intervalTicks == 1) {
            return true;
        }
        return Math.floorMod(gameTime + positionHash, intervalTicks) == 0;
    }
}