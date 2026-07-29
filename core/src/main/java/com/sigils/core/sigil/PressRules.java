package com.sigils.core.sigil;

/**
 * What a spell press costs and what stops it.
 *
 * <p>Split from the block for the usual reason — the decision has an order to it,
 * and an order is a thing you can get wrong silently. A press that reports
 * "no ink" when the real problem is that its target is walled in sends someone
 * to the wrong shelf.
 */
public final class PressRules {

    /** Ink units spent per stamp. One magical-ink item is 4 units. */
    public static final float INK_PER_STAMP = 4f;

    /** Ticks a press waits between stamps, however fast the clock feeding it runs. */
    public static final int COOLDOWN_TICKS = 10;

    /** How far a press looks when counting its neighbours. */
    public static final int NEARBY_RADIUS = 16;

    /**
     * How many sigils may already exist within {@link #NEARBY_RADIUS} before a
     * press stops adding to them.
     *
     * <p>Not balance — a brake. A press on a fast clock is a block entity factory,
     * and the roadmap's warning about this phase is that placed sigils are the
     * thing that gets expensive. The limit is high enough that no one building
     * deliberately will meet it and low enough that nobody's server dies to a
     * forgotten repeater.
     */
    public static final int NEARBY_LIMIT = 32;

    private PressRules() {}

    /**
     * @param template     an inscribed parchment is loaded
     * @param pen          a pen is loaded
     * @param inkless      the pen's {@code inklessOnSolids} — it doesn't spend ink
     * @param inkAvailable units in the ink slot, from {@code InkSupply.capacityOf}
     * @param targetClear  the cell in front can hold a sigil
     * @param nearby       sigils already within {@link #NEARBY_RADIUS}
     */
    public static PressReadiness evaluate(boolean template, boolean pen, boolean inkless,
                                          float inkAvailable, boolean targetClear, int nearby) {
        if (!template) {
            return PressReadiness.NO_TEMPLATE;
        }
        if (!pen) {
            return PressReadiness.NO_PEN;
        }
        // An inkless nib still needs ink present, because ink is what decides the
        // sigil's colour and whether rain takes it. It simply never runs out.
        if (inkAvailable <= 0f) {
            return PressReadiness.NO_INK;
        }
        if (!inkless && inkAvailable < INK_PER_STAMP) {
            return PressReadiness.NO_INK;
        }
        if (!targetClear) {
            return PressReadiness.OBSTRUCTED;
        }
        if (nearby >= NEARBY_LIMIT) {
            return PressReadiness.CROWDED;
        }
        return PressReadiness.READY;
    }
}