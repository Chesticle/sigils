package com.sigils.circuit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The one question every trigger answers: is the ring closed?
 *
 * <p>The roadmap's whole argument for this phase, in one method. A pressure plate,
 * a redstone signal, a piston pushing the final arc into place, a tripwire, a
 * timer and an entity crossing a threshold are not six features — they are six
 * implementations of this, and the fifth one costs nine lines because the first
 * one had to decide what "closed" means.
 */
@FunctionalInterface
public interface CircuitCompletion {

    /** Never closes. What an unknown trigger id resolves to. */
    CircuitCompletion NEVER = new CircuitCompletion() {
        @Override
        public boolean isClosed(CircuitSite site) {
            return false;
        }

        @Override
        public int pollInterval() {
            return 0;
        }
    };

    boolean isClosed(CircuitSite site);

    /**
     * How often this trigger needs asking, in ticks.
     *
     * <p><b>0 means never poll it.</b> Something else wakes it — a block update,
     * an entity event — and until then, asking is wasted work. Redstone and
     * physical closure are both in this class: moving a block or changing a
     * signal <em>is</em> a neighbour update, and Part B re-evaluates on one.
     *
     * <p>Anything above 0 is polled on a schedule staggered by position, so a
     * hundred sigils sharing an interval do not share a tick. Keep it as high as
     * the trigger can tolerate: this number is the phase's performance budget,
     * declared by the thing that spends it.
     */
    default int pollInterval() {
        return 4;
    }

    /**
     * Is the circuit closed at <em>any</em> of these cells?
     *
     * <p>A multiblock sigil is closed when anything closes any part of it — step
     * on the ring anywhere and the circle wakes. The default composes
     * {@link #isClosed} and is correct for every trigger; override it only when
     * asking once about the whole footprint is cheaper than asking N times, which
     * for anything that queries entities it very much is.
     *
     * @param footprint every cell, core first; never empty
     * @param radius    the structure's ring radius, passed on to each site
     */
    default boolean isClosedAnywhere(Level level, List<BlockPos> footprint,
                                     Direction face, int radius) {
        for (BlockPos cell : footprint) {
            if (isClosed(new CircuitSite(level, cell, face, radius))) {
                return true;
            }
        }
        return false;
    }
}