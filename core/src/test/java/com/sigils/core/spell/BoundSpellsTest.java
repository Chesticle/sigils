package com.sigils.core.spell;

import com.sigils.core.element.ElementalMixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoundSpellsTest {

    /** Spells distinguishable by their fidelity, which is all these tests need. */
    private static CompiledSpell spell(float fidelity) {
        return new CompiledSpell(
                1,
                ElementalMixture.of("sigils:fire", 1f),
                new Delivery("sigils:beam", 1f, 0, "sigils:self"),
                fidelity,
                List.of("sigils:ring_basic"));
    }

    private static BoundSpells bookOf(int pages) {
        BoundSpells book = BoundSpells.EMPTY;
        for (int i = 0; i < pages; i++) {
            book = book.bind(spell(0.1f * (i + 1)));
        }
        return book;
    }

    @Test
    @DisplayName("an empty book has nothing to cast")
    void emptyHasNoActiveSpell() {
        assertTrue(BoundSpells.EMPTY.isEmpty());
        assertTrue(BoundSpells.EMPTY.activeSpell().isEmpty());
    }

    @Test
    @DisplayName("binding appends and selects what was just bound")
    void bindSelectsTheNewPage() {
        BoundSpells book = bookOf(3);

        assertEquals(3, book.size());
        assertEquals(2, book.active());
        assertEquals(0.3f, book.activeSpell().orElseThrow().fidelity(), 1e-5);
    }

    @Test
    @DisplayName("a full book refuses another page rather than dropping one")
    void bindingPastCapacityIsANoOp() {
        BoundSpells full = bookOf(BoundSpells.MAX_PAGES);

        assertTrue(full.full());
        assertSame(full, full.bind(spell(0.99f)));
    }

    @Test
    @DisplayName("cycling forward wraps past the last page")
    void cycleForwardWraps() {
        BoundSpells book = bookOf(3); // active = 2

        assertEquals(0, book.cycled(1).active());
        assertEquals(1, book.cycled(1).cycled(1).active());
    }

    @Test
    @DisplayName("cycling backward wraps past the first page")
    void cycleBackwardWraps() {
        BoundSpells book = bookOf(3).cycled(1); // active = 0

        assertEquals(2, book.cycled(-1).active());
    }

    @Test
    @DisplayName("a one-page book cycles to itself")
    void singlePageIsStable() {
        BoundSpells book = bookOf(1);

        assertEquals(0, book.cycled(1).active());
        assertEquals(0, book.cycled(-1).active());
    }

    @Test
    @DisplayName("an out-of-range selection is clamped, not honoured")
    void activeIndexIsClamped() {
        assertEquals(1, new BoundSpells(List.of(spell(0.1f), spell(0.2f)), 47).active());
        assertEquals(0, new BoundSpells(List.of(), 47).active());
    }

    @Test
    @DisplayName("removing a page keeps the selection near where it was")
    void removalMovesTheSelectionSensibly() {
        BoundSpells book = bookOf(3);          // active = 2
        assertEquals(1, book.without(2).active());
        assertEquals(1, book.without(0).active());
        assertEquals(0, bookOf(1).without(0).active());
    }
}