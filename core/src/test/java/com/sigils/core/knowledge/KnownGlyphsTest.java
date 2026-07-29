package com.sigils.core.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KnownGlyphsTest {

    @Test
    @DisplayName("a new player knows nothing")
    void emptyByDefault() {
        assertTrue(KnownGlyphs.NONE.isEmpty());
        assertFalse(KnownGlyphs.NONE.knows("sigils:mod_beam"));
        assertEquals(0, KnownGlyphs.NONE.size());
    }

    @Test
    @DisplayName("learning a glyph makes it known")
    void learningAdds() {
        KnownGlyphs known = KnownGlyphs.NONE.learned("sigils:mod_beam");

        assertTrue(known.knows("sigils:mod_beam"));
        assertFalse(known.knows("sigils:mod_wide"));
        assertEquals(1, known.size());
    }

    @Test
    @DisplayName("learning the same glyph twice returns the very same instance")
    void learningTwiceIsIdentity() {
        KnownGlyphs once = KnownGlyphs.NONE.learned("sigils:mod_beam");

        assertSame(once, once.learned("sigils:mod_beam"));
    }

    @Test
    @DisplayName("forgetting removes it, and forgetting an unknown one changes nothing")
    void forgetting() {
        KnownGlyphs known = KnownGlyphs.NONE.learned("sigils:mod_beam");

        assertFalse(known.forgotten("sigils:mod_beam").knows("sigils:mod_beam"));
        assertSame(known, known.forgotten("sigils:crest_water"));
    }

    @Test
    @DisplayName("and() unions the two sets")
    void unions() {
        KnownGlyphs learned = KnownGlyphs.NONE.learned("sigils:mod_beam");
        KnownGlyphs innate = new KnownGlyphs(Set.of("sigils:ring_basic", "sigils:crest_fire"));

        KnownGlyphs both = learned.and(innate);

        assertEquals(3, both.size());
        assertTrue(both.knows("sigils:mod_beam"));
        assertTrue(both.knows("sigils:ring_basic"));
    }

    @Test
    @DisplayName("and() returns the receiver when the other side adds nothing")
    void unionOfSubsetIsIdentity() {
        KnownGlyphs known = new KnownGlyphs(Set.of("sigils:ring_basic", "sigils:crest_fire"));

        assertSame(known, known.and(new KnownGlyphs(Set.of("sigils:crest_fire"))));
        assertSame(known, known.and(KnownGlyphs.NONE));
    }

    @Test
    @DisplayName("the set is copied, so the caller cannot mutate it afterwards")
    void constructorCopies() {
        Set<String> mutable = new HashSet<>(Set.of("sigils:ring_basic"));
        KnownGlyphs known = new KnownGlyphs(mutable);

        mutable.add("sigils:mod_grand");

        assertFalse(known.knows("sigils:mod_grand"));
        assertEquals(1, known.size());
    }

    @Test
    @DisplayName("sorted() is alphabetical regardless of insertion order")
    void sortedIsStable() {
        KnownGlyphs known = KnownGlyphs.NONE
                .learned("sigils:mod_beam")
                .learned("sigils:crest_fire")
                .learned("sigils:ring_basic");

        assertEquals(
                List.of("sigils:crest_fire", "sigils:mod_beam", "sigils:ring_basic"),
                known.sorted());
    }
}