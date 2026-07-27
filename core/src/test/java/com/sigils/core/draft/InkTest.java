package com.sigils.core.draft;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InkTest {

    private static final StrokePath LINE = StrokePath.of(new Vec2(0f, 0.5f), new Vec2(1f, 0.5f));

    private static final Map<String, Glyph> GLYPHS = Map.of(
            "test:crest", new Glyph("test:crest", GlyphRole.CREST, List.of(LINE), 0.05f, 1, 2f,
                    Optional.of(ElementalMixture.of("sigils:fire", 1f)), Optional.empty()),
            "test:ring", new Glyph("test:ring", GlyphRole.RING, List.of(LINE), 0.05f, 1, 1f,
                    Optional.empty(), Optional.empty()));

    private static final GlyphLookup LOOKUP = id -> Optional.ofNullable(GLYPHS.get(id));

    @Test
    @DisplayName("cost is each glyph's ink cost times how big it was drawn")
    void costScalesWithPlacement() {
        List<GlyphInstance> draft = List.of(
                new GlyphInstance("test:ring", new Vec2(0.5f, 0.5f), 0f, 1f),    // 1.0 x 1.0
                new GlyphInstance("test:crest", new Vec2(0.5f, 0.5f), 0f, 0.5f)); // 2.0 x 0.5

        assertEquals(2f, InkCost.of(draft, LOOKUP), 1e-5);
    }

    @Test
    @DisplayName("a placement referencing an unknown glyph costs nothing")
    void unknownGlyphCostsNothing() {
        List<GlyphInstance> draft =
                List.of(new GlyphInstance("test:nonexistent", new Vec2(0.5f, 0.5f), 0f, 1f));

        assertEquals(0f, InkCost.of(draft, LOOKUP), 1e-5);
    }

    @Test
    @DisplayName("an overdraw is refused whole — the balance is untouched")
    void ledgerRefusesOverdraw() {
        InkLedger ledger = new InkLedger(2f);

        assertTrue(ledger.charge(1.5f));
        assertFalse(ledger.charge(1f), "0.5 left cannot cover 1.0");
        assertEquals(0.5f, ledger.remaining(), 1e-5);
        assertEquals(0.25f, ledger.fractionRemaining(), 1e-5);
        assertFalse(ledger.dry());
    }
}