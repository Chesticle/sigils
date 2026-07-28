package com.sigils.circuit;

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
}