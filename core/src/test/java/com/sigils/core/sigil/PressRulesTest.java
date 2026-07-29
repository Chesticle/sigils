package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PressRulesTest {

    /** A press with nothing wrong with it. */
    private static PressReadiness press(boolean template, boolean pen, boolean inkless,
                                        float ink, boolean clear, int nearby) {
        return PressRules.evaluate(template, pen, inkless, ink, clear, nearby);
    }

    @Test
    @DisplayName("a fully loaded press with a free surface is ready")
    void loadedPressIsReady() {
        assertEquals(PressReadiness.READY,
                press(true, true, false, PressRules.INK_PER_STAMP, true, 0));
    }

    @Test
    @DisplayName("reasons arrive in the order you would act on them")
    void reasonsArePrioritised() {
        // Everything is wrong at once. The message should name the first thing
        // you'd go and fix, not whichever check happened to run first.
        assertEquals(PressReadiness.NO_TEMPLATE, press(false, false, false, 0f, false, 999));
        assertEquals(PressReadiness.NO_PEN, press(true, false, false, 0f, false, 999));
        assertEquals(PressReadiness.NO_INK, press(true, true, false, 0f, false, 999));
        assertEquals(PressReadiness.OBSTRUCTED, press(true, true, false, 99f, false, 999));
        assertEquals(PressReadiness.CROWDED, press(true, true, false, 99f, true, 999));
    }

    @Test
    @DisplayName("an ordinary nib needs a full charge, not a trace")
    void ordinaryNibNeedsAFullCharge() {
        assertEquals(PressReadiness.NO_INK,
                press(true, true, false, PressRules.INK_PER_STAMP - 0.5f, true, 0));
    }

    @Test
    @DisplayName("an inkless nib prints on a trace of ink")
    void inklessNibPrintsOnAlmostNothing() {
        assertEquals(PressReadiness.READY, press(true, true, true, 0.1f, true, 0));
    }

    @Test
    @DisplayName("an inkless nib still needs some ink, for the colour")
    void inklessNibStillNeedsAGrade() {
        assertEquals(PressReadiness.NO_INK, press(true, true, true, 0f, true, 0));
    }

    @Test
    @DisplayName("the crowding brake trips at the limit, not past it")
    void crowdingTripsAtTheLimit() {
        assertEquals(PressReadiness.READY,
                press(true, true, false, 99f, true, PressRules.NEARBY_LIMIT - 1));
        assertEquals(PressReadiness.CROWDED,
                press(true, true, false, 99f, true, PressRules.NEARBY_LIMIT));
    }
}