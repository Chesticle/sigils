package com.sigils.core.draft;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.*;
import com.sigils.core.spell.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DraftValidatorTest {

    private static final StrokePath LINE = StrokePath.of(new Vec2(0f, 0.5f), new Vec2(1f, 0.5f));

    private static Glyph crest(String id, int complexity) {
        return new Glyph(id, GlyphRole.CREST, List.of(LINE), 0.05f, complexity, 2f,
                Optional.of(ElementalMixture.of("sigils:fire", 1f)), Optional.empty());
    }

    private static final Map<String, Glyph> GLYPHS = Map.of(
            "test:crest_fire", crest("test:crest_fire", 2),
            "test:crest_water", crest("test:crest_water", 2),
            "test:crest_baroque", crest("test:crest_baroque", 9),
            "test:mod_beam", new Glyph("test:mod_beam", GlyphRole.MODIFIER, List.of(LINE),
                    0.05f, 1, 1f, Optional.empty(),
                    Optional.of(new ModifierOp.Shape("sigils:beam"))),
            "test:ring", new Glyph("test:ring", GlyphRole.RING, List.of(LINE),
                    0.05f, 1, 1f, Optional.empty(), Optional.empty()));

    private static final GlyphLookup LOOKUP = id -> Optional.ofNullable(GLYPHS.get(id));

    private static GlyphInstance at(String id, float x, float y) {
        return new GlyphInstance(id, new Vec2(x, y), 0f, 0.3f);
    }

    private static boolean mentions(ValidationResult result, String fragment) {
        return result.errors().stream().anyMatch(e -> e.contains(fragment));
    }

    @Test
    @DisplayName("crest + modifier + ring on the table is a valid draft")
    void wellFormedDraftValidates() {
        List<GlyphInstance> draft = List.of(
                at("test:ring", 0.5f, 0.5f),
                at("test:crest_fire", 0.5f, 0.5f),
                at("test:mod_beam", 0.5f, 0.68f));

        ValidationResult result =
                DraftValidator.validate(draft, LOOKUP, DraftLimits.DRAFTING_TABLE);

        assertTrue(result.valid(), () -> String.join(" / ", result.errors()));
    }

    @Test
    @DisplayName("a ring with nothing in it reports the missing crest")
    void missingCrestReported() {
        ValidationResult result = DraftValidator.validate(
                List.of(at("test:ring", 0.5f, 0.5f)), LOOKUP, DraftLimits.DRAFTING_TABLE);

        assertFalse(result.valid());
        assertTrue(mentions(result, "crest"));
    }

    @Test
    @DisplayName("two crests exceed what today's pen can bind")
    void tooManyCrestsReported() {
        List<GlyphInstance> draft = List.of(
                at("test:ring", 0.5f, 0.5f),
                at("test:crest_fire", 0.42f, 0.5f),
                at("test:crest_water", 0.58f, 0.5f));

        ValidationResult result =
                DraftValidator.validate(draft, LOOKUP, DraftLimits.DRAFTING_TABLE);

        assertFalse(result.valid());
        assertTrue(mentions(result, "crest(s) in one circle"));
    }

    @Test
    @DisplayName("a placement referencing a glyph the server doesn't have is named")
    void unknownGlyphReported() {
        List<GlyphInstance> draft = List.of(
                at("test:ring", 0.5f, 0.5f),
                at("test:crest_fire", 0.5f, 0.5f),
                at("test:crest_of_the_ancients", 0.5f, 0.6f));

        ValidationResult result =
                DraftValidator.validate(draft, LOOKUP, DraftLimits.DRAFTING_TABLE);

        assertFalse(result.valid());
        assertTrue(mentions(result, "Unknown glyph: test:crest_of_the_ancients"));
    }

    @Test
    @DisplayName("a glyph dragged past the edge of the circle is caught")
    void outsideTheCircleReported() {
        List<GlyphInstance> draft = List.of(
                at("test:ring", 0.5f, 0.5f),
                at("test:crest_fire", 0.99f, 0.5f));

        ValidationResult result =
                DraftValidator.validate(draft, LOOKUP, DraftLimits.DRAFTING_TABLE);

        assertFalse(result.valid());
        assertTrue(mentions(result, "sits outside the circle"));
    }

    @Test
    @DisplayName("the notepad refuses a glyph too intricate for it")
    void tooComplexForTheNotepad() {
        List<GlyphInstance> draft = List.of(
                at("test:ring", 0.5f, 0.5f),
                at("test:crest_baroque", 0.5f, 0.5f));

        ValidationResult result =
                DraftValidator.validate(draft, LOOKUP, DraftLimits.NOTEPAD);

        assertFalse(result.valid());
        assertTrue(mentions(result, "too intricate"));
    }
}