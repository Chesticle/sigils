package com.sigils.core.particle;

import com.sigils.core.element.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElementVisualsTest {

    @Test
    @DisplayName("a bright, rising element glows and lifts — with no authored data")
    void risingBrightElement() {
        // density < 0 rises; luminance 1 glows
        Element ember = new Element("sigils:ember", 0xFF6600, -1.0f, 0.8f, 1.0f);
        ParticleProfile p = ElementVisuals.profileFor(ember);
        assertTrue(p.gravity() < 0f, "negative density should lift");
        assertEquals(1.0f, p.emissive(), 1e-5, "luminance 1 should be fully emissive");
        assertTrue(p.red() > p.blue(), "colour should track the element's own hue");
    }

    @Test
    @DisplayName("a heavy, dark element settles and doesn't glow")
    void sinkingDarkElement() {
        Element silt = new Element("sigils:silt", 0x4A3B2A, 1.0f, 0.2f, 0.0f);
        ParticleProfile p = ElementVisuals.profileFor(silt);
        assertTrue(p.gravity() > 0f, "positive density should settle");
        assertEquals(0f, p.emissive(), 1e-5, "no luminance, no glow");
    }
}