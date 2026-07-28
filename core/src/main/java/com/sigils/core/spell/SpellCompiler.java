package com.sigils.core.spell;

import com.sigils.core.SigilsCore;
import com.sigils.core.element.ElementalMixture;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.ModifierOp;
import com.sigils.core.trace.TraceResult;
import com.sigils.core.trace.TraceScores;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compiles a validated graph plus per-glyph trace results into a {@link
 * CompiledSpell}. Fails (with reasons) on any structural error, unknown glyph,
 * missing trace, or out-of-tolerance trace.
 *
 * <p>Traces are keyed by the {@link GlyphInstance} itself — records compare by
 * value, so the same placement resolves to the same trace.
 */
public final class SpellCompiler {

    public static final String DEFAULT_SHAPE = "sigils:burst";
    public static final String DEFAULT_TARGET = "sigils:self";
    public static final int DEFAULT_DURATION = 0;

    private final GlyphLookup glyphs;

    public SpellCompiler(GlyphLookup glyphs) {
        this.glyphs = glyphs;
    }

    public CompileResult compile(SpellGraph graph, Map<GlyphInstance, TraceResult> traces) {
        ValidationResult validation = SpellGraphValidator.validate(graph);
        if (!validation.valid()) {
            return new CompileResult.Failure(validation.errors());
        }

        List<String> errors = new ArrayList<>();

        // 1. Combine crest contributions, each scaled by how big it was drawn.
        ElementalMixture mixture = ElementalMixture.EMPTY;
        for (GlyphInstance crest : graph.crests()) {
            Optional<Glyph> glyph = glyphs.get(crest.glyphId());
            if (glyph.isEmpty()) {
                errors.add("Unknown crest glyph: " + crest.glyphId());
                continue;
            }
            ElementalMixture contribution = glyph.get().contribution().orElse(ElementalMixture.EMPTY);
            mixture = mixture.plus(contribution.scaled(crest.scale()));
        }

        // 2. Fold modifiers into a delivery description.
        String shape = DEFAULT_SHAPE;
        String target = DEFAULT_TARGET;
        float scale = 1f;
        for (GlyphInstance modInstance : graph.modifiers()) {
            Optional<Glyph> glyph = glyphs.get(modInstance.glyphId());
            if (glyph.isEmpty()) {
                errors.add("Unknown modifier glyph: " + modInstance.glyphId());
                continue;
            }
            ModifierOp op = glyph.get().operation().orElse(null);
            // A sealed interface makes this exhaustive: add a new ModifierOp kind
            // and the compiler is where you'll be reminded to handle it.
            if (op instanceof ModifierOp.Shape s) {
                shape = s.shapeId();
            } else if (op instanceof ModifierOp.Target t) {
                target = t.targetId();
            } else if (op instanceof ModifierOp.Scale sc) {
                scale *= sc.factor() * modInstance.scale();
            }
        }

        // 3. Aggregate fidelity across every glyph; any invalid trace fails the spell.
        List<GlyphInstance> all = new ArrayList<>();
        all.addAll(graph.crests());
        all.addAll(graph.modifiers());
        all.addAll(graph.rings());

        List<TraceResult> scored = new ArrayList<>(all.size());
        for (GlyphInstance instance : all) {
            TraceResult result = traces.get(instance);
            if (result == null) {
                errors.add("Missing trace for placement: " + instance.glyphId());
                continue;
            }
            if (!result.valid()) {
                errors.add("Trace for '" + instance.glyphId() + "' is out of tolerance.");
            }
            scored.add(result);
        }

        if (!errors.isEmpty()) {
            return new CompileResult.Failure(errors);
        }

        float fidelity = TraceScores.mean(scored);

        List<String> ringIds = new ArrayList<>();
        for (GlyphInstance ring : graph.rings()) {
            ringIds.add(ring.glyphId());
        }

        Delivery delivery = new Delivery(shape, scale, DEFAULT_DURATION, target);
        CompiledSpell spell = new CompiledSpell(
                SigilsCore.SPELL_SCHEMA_VERSION, mixture, delivery, fidelity, ringIds);
        return new CompileResult.Success(spell);
    }
}