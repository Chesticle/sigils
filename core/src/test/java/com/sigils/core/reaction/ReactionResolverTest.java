package com.sigils.core.reaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import com.sigils.core.element.ElementalMixture;

import static org.junit.jupiter.api.Assertions.*;

class ReactionResolverTest {

    private static final String FIRE = "sigils:fire";
    private static final String WATER = "sigils:water";
    private static final String EARTH = "sigils:earth";
    private static final String AIR = "sigils:air";

    private static final String STEAM = "sigils:steam";
    private static final String SCORCH = "sigils:scorch";

    private final PhenomenonResolver resolver = new PhenomenonResolver();

    // A steam reaction: 1 fire + 1 water -> 1 steam, at the given priority.
    private static ReactionRule steam(int priority) {
        return new ReactionRule(STEAM,
                List.of(Reagent.of(FIRE, 1f), Reagent.of(WATER, 1f)),
                STEAM, 1f, priority);
    }

    private static ElementalMixture mix(String a, float av, String b, float bv) {
        return ElementalMixture.of(a, av).plus(ElementalMixture.of(b, bv));
    }

    // ---------------------------------------------------------------- the headline cases

    @Test
    @DisplayName("equal fire and water leave nothing but steam")
    void balancedSteam() {
        Resolution r = resolver.resolve(mix(FIRE, 1f, WATER, 1f), List.of(steam(10)));

        assertEquals(1f, r.strengthOf(STEAM), 1e-4, "one unit of steam");
        assertTrue(r.residual().isEmpty(), "nothing should be left over, got " + r.residual());
    }

    @Test
    @DisplayName("fire-heavy mixture makes less steam and leaves real fire behind")
    void fireHeavySteam() {
        // 1.5 fire, 0.5 water. Water is the limiting reagent, so only 0.5 units
        // of reaction happen: 0.5 steam is made and 1.0 fire survives to burn.
        Resolution r = resolver.resolve(mix(FIRE, 1.5f, WATER, 0.5f), List.of(steam(10)));

        assertEquals(0.5f, r.strengthOf(STEAM), 1e-4);
        assertEquals(1.0f, r.residual().amountOf(FIRE), 1e-4, "excess fire remains");
        assertFalse(r.residual().contains(WATER), "water was fully consumed");
    }

    @Test
    @DisplayName("water-heavy mixture is the mirror image")
    void waterHeavySteam() {
        Resolution r = resolver.resolve(mix(FIRE, 0.5f, WATER, 1.5f), List.of(steam(10)));

        assertEquals(0.5f, r.strengthOf(STEAM), 1e-4);
        assertEquals(1.0f, r.residual().amountOf(WATER), 1e-4);
        assertFalse(r.residual().contains(FIRE));
    }

    // ---------------------------------------------------------------- mechanics

    @Test
    @DisplayName("yield scales the phenomenon strength")
    void yieldScaling() {
        ReactionRule strongSteam = new ReactionRule(STEAM,
                List.of(Reagent.of(FIRE, 1f), Reagent.of(WATER, 1f)),
                STEAM, 2.0f, 10); // yield 2

        Resolution r = resolver.resolve(mix(FIRE, 1f, WATER, 1f), List.of(strongSteam));
        assertEquals(2.0f, r.strengthOf(STEAM), 1e-4);
    }

    @Test
    @DisplayName("asymmetric consumption respects the limiting reagent")
    void asymmetricConsumption() {
        // This reaction eats 2 fire per 1 water.
        ReactionRule rule = new ReactionRule(STEAM,
                List.of(Reagent.of(FIRE, 2f), Reagent.of(WATER, 1f)),
                STEAM, 1f, 10);

        // fire 2, water 2: fire allows 2/2=1 reaction, water allows 2/1=2.
        // Fire limits at 1 reaction -> consumes 2 fire + 1 water, leaves 1 water.
        Resolution r = resolver.resolve(mix(FIRE, 2f, WATER, 2f), List.of(rule));
        assertEquals(1f, r.strengthOf(STEAM), 1e-4);
        assertEquals(1f, r.residual().amountOf(WATER), 1e-4);
        assertFalse(r.residual().contains(FIRE));
    }

    @Test
    @DisplayName("unreacted elements pass through as residual")
    void passThrough() {
        // Air has no rule; it should survive untouched alongside the steam.
        ElementalMixture input = mix(FIRE, 1f, WATER, 1f).plus(ElementalMixture.of(AIR, 2f));

        Resolution r = resolver.resolve(input, List.of(steam(10)));
        assertEquals(1f, r.strengthOf(STEAM), 1e-4);
        assertEquals(2f, r.residual().amountOf(AIR), 1e-4);
    }

    @Test
    @DisplayName("a mixture with no matching rule reacts to nothing")
    void noMatchingRule() {
        Resolution r = resolver.resolve(ElementalMixture.of(AIR, 3f), List.of(steam(10)));
        assertTrue(r.isInert(), "no reaction should fire");
        assertEquals(3f, r.residual().amountOf(AIR), 1e-4, "the air is untouched");
    }

    @Test
    @DisplayName("an empty mixture yields an empty resolution")
    void emptyInput() {
        Resolution r = resolver.resolve(ElementalMixture.EMPTY, List.of(steam(10)));
        assertTrue(r.isInert());
        assertTrue(r.residual().isEmpty());
    }

    // ---------------------------------------------------------------- multi-rule behaviour

    @Test
    @DisplayName("higher-priority rules claim shared reagents first")
    void priorityAndCascade() {
        // Two rules both want fire. Steam (priority 10) resolves before
        // scorch (priority 5). With fire 2, water 1, earth 1:
        //   steam eats 1 fire + 1 water  -> steam 1, pool now fire 1, earth 1
        //   scorch eats 1 fire + 1 earth -> scorch 1, pool now empty
        ReactionRule scorch = new ReactionRule(SCORCH,
                List.of(Reagent.of(FIRE, 1f), Reagent.of(EARTH, 1f)),
                SCORCH, 1f, 5);

        ElementalMixture input = ElementalMixture.of(FIRE, 2f)
                .plus(ElementalMixture.of(WATER, 1f))
                .plus(ElementalMixture.of(EARTH, 1f));

        Resolution r = resolver.resolve(input, List.of(steam(10), scorch));

        assertEquals(1f, r.strengthOf(STEAM), 1e-4);
        assertEquals(1f, r.strengthOf(SCORCH), 1e-4);
        assertTrue(r.residual().isEmpty(), "everything reacted, got " + r.residual());
    }

    @Test
    @DisplayName("resolution is identical regardless of rule input order")
    void deterministicOrdering() {
        ReactionRule scorch = new ReactionRule(SCORCH,
                List.of(Reagent.of(FIRE, 1f), Reagent.of(EARTH, 1f)),
                SCORCH, 5f, 5);

        ElementalMixture input = ElementalMixture.of(FIRE, 2f)
                .plus(ElementalMixture.of(WATER, 1f))
                .plus(ElementalMixture.of(EARTH, 1f));

        Resolution forwards = resolver.resolve(input, List.of(steam(10), scorch));
        Resolution backwards = resolver.resolve(input, List.of(scorch, steam(10)));

        assertEquals(forwards.phenomena(), backwards.phenomena());
        assertEquals(forwards.residual(), backwards.residual());
    }

    // ---------------------------------------------------------------- ratio windows

    @Test
    @DisplayName("a ratio-gated reaction only fires in its proportion window")
    void ratioWindow() {
        // Steam only forms when fire is 40-60% of the mixture.
        ReactionRule gated = new ReactionRule(STEAM,
                List.of(new Reagent(FIRE, 0.4f, 0.6f, 1f), Reagent.of(WATER, 1f)),
                STEAM, 1f, 10);

        // 1:1 -> fire is 50%, inside the window -> fires.
        Resolution balanced = resolver.resolve(mix(FIRE, 1f, WATER, 1f), List.of(gated));
        assertTrue(balanced.hasPhenomenon(STEAM), "50% fire should react");

        // 3:1 -> fire is 75%, outside the window -> does not fire, nothing consumed.
        Resolution tooMuchFire = resolver.resolve(mix(FIRE, 3f, WATER, 1f), List.of(gated));
        assertFalse(tooMuchFire.hasPhenomenon(STEAM), "75% fire is out of window");
        assertEquals(3f, tooMuchFire.residual().amountOf(FIRE), 1e-4);
        assertEquals(1f, tooMuchFire.residual().amountOf(WATER), 1e-4);

        // 1:3 -> fire is 25%, outside the window -> does not fire.
        Resolution tooMuchWater = resolver.resolve(mix(FIRE, 1f, WATER, 3f), List.of(gated));
        assertFalse(tooMuchWater.hasPhenomenon(STEAM), "25% fire is out of window");
    }
}