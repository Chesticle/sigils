package com.sigils.core.sigil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SigilFootprintTest {

    @Test
    @DisplayName("radius 0 is a lone cell")
    void radiusZeroHasNoRing() {
        assertTrue(SigilFootprint.ring(0).isEmpty());
        assertEquals(List.of(PlaneOffset.CENTRE), SigilFootprint.all(0));
    }

    @Test
    @DisplayName("every cell on the ring is at the ring's radius")
    void cellsSitOnTheRadius() {
        for (int r = 1; r <= 6; r++) {
            for (PlaneOffset cell : SigilFootprint.ring(r)) {
                long distance = Math.round(
                        Math.sqrt((double) cell.a() * cell.a() + (double) cell.b() * cell.b()));
                assertEquals(r, distance, "cell " + cell + " on ring " + r);
            }
        }
    }

    @Test
    @DisplayName("the ring is a closed loop with no gaps")
    void theRingIsClosed() {
        // Every cell must touch at least two others, or the circle has a break in
        // it — which for this mod is the one geometric property that matters.
        for (int r = 1; r <= 8; r++) {
            List<PlaneOffset> ring = SigilFootprint.ring(r);
            for (PlaneOffset cell : ring) {
                int neighbours = 0;
                for (PlaneOffset other : ring) {
                    if (other.equals(cell)) {
                        continue;
                    }
                    if (Math.abs(other.a() - cell.a()) <= 1 && Math.abs(other.b() - cell.b()) <= 1) {
                        neighbours++;
                    }
                }
                assertTrue(neighbours >= 2,
                        "cell " + cell + " on ring " + r + " has only " + neighbours + " neighbours");
            }
        }
    }

    @Test
    @DisplayName("the ring is symmetric about both axes")
    void theRingIsSymmetric() {
        List<PlaneOffset> ring = SigilFootprint.ring(4);
        for (PlaneOffset cell : ring) {
            assertTrue(ring.contains(new PlaneOffset(-cell.a(), cell.b())), "mirror of " + cell);
            assertTrue(ring.contains(new PlaneOffset(cell.a(), -cell.b())), "mirror of " + cell);
            assertTrue(ring.contains(new PlaneOffset(cell.b(), cell.a())), "transpose of " + cell);
        }
    }

    @Test
    @DisplayName("a radius past the cap is clamped, not honoured")
    void oversizeRadiusIsClamped() {
        assertEquals(SigilFootprint.ring(SigilFootprint.MAX_RADIUS).size(),
                SigilFootprint.ring(999).size());
        assertTrue(SigilFootprint.ring(-4).isEmpty());
    }
}