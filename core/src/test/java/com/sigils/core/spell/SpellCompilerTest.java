package com.sigils.core.spell;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.ModifierOp;
import com.sigils.core.trace.TraceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpellCompilerTest {

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

    private static final SpellCompiler COMPILER = new SpellCompiler(LOOKUP);

    // A clean, valid trace result for any glyph.
    private static TraceResult good() {
        return new TraceResult(true, 0.9f, 0.95f, 0.01f, 0.05f);
    }

    @Test
    @DisplayName("a fire crest + beam modifier + ring compiles to a fire beam")
    void happyPath() {
        GlyphInstance crest = new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f);
        GlyphInstance beam = new GlyphInstance("sigils:beam", new Vec2(0.55f, 0.5f), 0f, 1f);
        GlyphInstance ring = new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f);

        SpellGraph graph = SpellGraphBuilder.build(List.of(crest, beam, ring), LOOKUP);

        Map<GlyphInstance, TraceResult> traces = new HashMap<>();
        traces.put(crest, good());
        traces.put(beam, good());
        traces.put(ring, good());

        CompileResult result = COMPILER.compile(graph, traces);
        assertInstanceOf(CompileResult.Success.class, result);

        CompiledSpell spell = ((CompileResult.Success) result).spell();
        assertEquals(1f, spell.mixture().amountOf("sigils:fire"), 1e-4);
        assertEquals("sigils:beam", spell.delivery().shapeId());
        assertEquals("sigils:self", spell.delivery().targetId()); // default, no target modifier
        assertEquals(0.9f, spell.fidelity(), 1e-4);               // mean of three 0.9s
        assertEquals(SigilsCoreSchema(), spell.schemaVersion());
    }

    // Small helper so the test reads clearly without importing the constant inline.
    private static int SigilsCoreSchema() {
        return com.sigils.core.SigilsCore.SPELL_SCHEMA_VERSION;
    }

    @Test
    @DisplayName("a spell with no ring fails to compile")
    void missingRingFails() {
        GlyphInstance crest = new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f);
        SpellGraph graph = SpellGraphBuilder.build(List.of(crest), LOOKUP);

        CompileResult result = COMPILER.compile(graph, Map.of(crest, good()));
        assertInstanceOf(CompileResult.Failure.class, result);
    }

    @Test
    @DisplayName("an out-of-tolerance trace fails to compile with a reason")
    void invalidTraceFails() {
        GlyphInstance crest = new GlyphInstance("sigils:fire_crest", new Vec2(0.5f, 0.5f), 0f, 1f);
        GlyphInstance ring = new GlyphInstance("sigils:ring", new Vec2(0.5f, 0.5f), 0f, 1f);
        SpellGraph graph = SpellGraphBuilder.build(List.of(crest, ring), LOOKUP);

        Map<GlyphInstance, TraceResult> traces = new HashMap<>();
        traces.put(crest, good());
        traces.put(ring, TraceResult.FAILED); // botched the ring

        CompileResult result = COMPILER.compile(graph, traces);
        assertInstanceOf(CompileResult.Failure.class, result);
        List<String> errors = ((CompileResult.Failure) result).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("out of tolerance")));
    }
}