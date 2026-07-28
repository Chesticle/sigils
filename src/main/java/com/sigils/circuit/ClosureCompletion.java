package com.sigils.circuit;

/**
 * Closed while something physically occupies the cell in front of the drawn
 * surface.
 *
 * <p>The most literal reading of the roadmap's sentence — the ring really is
 * incomplete until a block finishes it — and the one that makes a piston an arming
 * mechanism rather than a redstone source. Extend the piston, the sigil arms and
 * fires; retract it, and the sigil is disarmed until next time.
 */
public final class ClosureCompletion implements CircuitCompletion {

    @Override
    public boolean isClosed(CircuitSite site) {
        return !site.level().getBlockState(site.front()).isAir();
    }

    @Override
    public int pollInterval() {
        return 0; // a block moving is a neighbour update
    }
}