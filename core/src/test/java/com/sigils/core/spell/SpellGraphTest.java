package com.sigils.core.spell;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.ModifierOp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpellGraphTest {

    // A minimal stroke every test glyph can share.
    private static final List<StrokePath> STROKE =
            List.of(StrokePath.of(new Vec2(0f, 0f), new Vec2(1f, 0f)));

    private static final Glyph FIRE_CREST = new Glyph(
            "sigils:fire_crest", GlyphRole.CREST, STROKE, 0.1f, 1, 1f,
            Optional.of(ElementalMixture.of("sigils:fire", 1f)), Optional.empty());

    private static final Glyph BEAM_MOD = new Glyph(
            "sigils:beam", GlyphRole.MODIFIER, STROKE, 0.1f, 1, 1f,
            Optional.empty(), Optional.of(new ModifierOp.Shape("sigils:beam")));

    private static final Glyph RING = new Glyph(
            "sigils:ring", GlyphRole.RING, STROKE, 0.1f, 1, 1f,
            Optional.empty(), Optional.empty());

    private static final GlyphLookup LOOKUP = id -> Optional.ofNullable(
            Map.of(FIRE_CREST.id(), FIRE_CREST, BEAM_MOD.id(), BEAM_MOD, RING.id(), RING).get(id));

    @Test
    @DisplayName("builder separates roles and links a modifier to its nearest crest")
    void buildsAndLinks() {
        List<GlyphInstance> placements = List.of(
                new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f),
                new GlyphInstance("sigils:beam", new Vec2(0.55f, 0.5f), 0f, 1f),
                new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f));

        SpellGraph graph = SpellGraphBuilder.build(placements, LOOKUP);

        assertEquals(1, graph.crests().size());
        assertEquals(1, graph.modifiers().size());
        assertEquals(1, graph.rings().size());
        assertEquals(1, graph.edges().size());
        assertEquals(0, graph.edges().get(0).crestIndex(), "modifier links to crest 0");
    }

    @Test
    @DisplayName("unknown glyph ids are skipped, not fatal")
    void skipsUnknownGlyphs() {
        List<GlyphInstance> placements = List.of(
                new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f),
                new GlyphInstance("sigils:does_not_exist", new Vec2(0.6f, 0.5f), 0f, 1f),
                new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f));

        SpellGraph graph = SpellGraphBuilder.build(placements, LOOKUP);
        assertEquals(1, graph.crests().size());
        assertEquals(0, graph.modifiers().size());
    }

    @Test
    @DisplayName("a well-formed graph validates")
    void validGraph() {
        SpellGraph graph = SpellGraphBuilder.build(List.of(
                new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f),
                new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f)), LOOKUP);

        assertTrue(SpellGraphValidator.validate(graph).valid());
    }

    @Test
    @DisplayName("a graph missing its ring fails validation with a helpful message")
    void missingRingFails() {
        SpellGraph graph = SpellGraphBuilder.build(List.of(
                new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f)), LOOKUP);

        ValidationResult result = SpellGraphValidator.validate(graph);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("ring")));
    }

    @Test
    @DisplayName("a graph with no crest fails validation")
    void missingCrestFails() {
        SpellGraph graph = SpellGraphBuilder.build(List.of(
                new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f)), LOOKUP);

        assertFalse(SpellGraphValidator.validate(graph).valid());
    }
}