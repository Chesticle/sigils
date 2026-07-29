package com.sigils.draft;

import com.sigils.core.draft.DraftQuality;
import com.sigils.core.knowledge.KnownGlyphs;
import com.sigils.core.spell.*;
import com.sigils.knowledge.SigilsKnowledge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sigils.Sigils;
import com.sigils.core.draft.DraftValidator;
import com.sigils.core.draft.InkCost;
import com.sigils.core.draft.StrokeQuantizer;
import com.sigils.core.geometry.RingGeometry;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.core.glyph.GlyphTransform;
import com.sigils.core.trace.TraceEvaluator;
import com.sigils.core.trace.TraceResult;
import com.sigils.menu.DraftingTableMenu;
import com.sigils.net.SpellDraftPayload;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsGlyphs;

/**
 * Turns a received draft into a spell on a parchment — or refuses to.
 *
 * <p>The client has already checked all of this. That is irrelevant: a packet is
 * whatever the sender chose to send, so every rule is applied again here, and
 * the traces are scored from raw samples rather than believed.
 */
public final class DraftInscriber {

    private DraftInscriber() {}

    public static void accept(SpellDraftPayload payload, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 1. Is this player actually standing at a drafting table, in the menu
        //    this packet claims to come from?
        if (!(serverPlayer.containerMenu instanceof DraftingTableMenu menu)
                || menu.containerId != payload.containerId()) {
            return;
        }

        // 2. Shapes must line up, and stay inside the caps the codec allowed.
        List<SpellDraftPayload.PlacedGlyph> sent = payload.placements();
        if (sent.isEmpty() || sent.size() != payload.traces().size()) {
            return;
        }

        // 3. The tools are whatever is in the block right now, not what the
        //    client remembers putting there.
        DraftContext context = menu.context();
        if (!context.ready()) {
            return;
        }

        // 4. Floats off the wire can be NaN or infinite. Records don't care;
        //    geometry very much does.
        List<GlyphInstance> placements = new ArrayList<>(sent.size());
        for (SpellDraftPayload.PlacedGlyph placed : sent) {
            if (!finite(placed.x(), placed.y(), placed.rotation(), placed.scale())
                    || placed.scale() <= 0f || placed.scale() > 1f) {
                return;
            }
            placements.add(new GlyphInstance(placed.glyphId(),
                    new Vec2(placed.x(), placed.y()), placed.rotation(), placed.scale()));
        }

        RegistryAccess registries = serverPlayer.level().registryAccess();
        GlyphLookup glyphs = SigilsGlyphs.lookup(registries);

        // 5. Every arrangement rule, again.
        ValidationResult validation =
                DraftValidator.validate(placements, glyphs, context.limits());
        if (!validation.valid()) {
            Sigils.LOGGER.debug("Rejected draft from {}: {}",
                    serverPlayer.getName().getString(), validation.errors());
            return;
        }

        // 5b. Every glyph has to be one this player has actually learned. The
        //     palette greys these out; a palette is not a permission system, and
        //     a packet is whatever the sender chose to send.
        KnownGlyphs known = SigilsKnowledge.effective(serverPlayer);
        for (GlyphInstance placement : placements) {
            if (!known.knows(placement.glyphId())) {
                Sigils.LOGGER.debug("Rejected draft from {}: unlearned glyph {}",
                        serverPlayer.getName().getString(), placement.glyphId());
                return;
            }
        }

        // 6. Could they have afforded it?
        float cost = InkCost.of(placements, glyphs);
        if (cost > context.inkCapacity()) {
            return;
        }

        // 7. Score every trace from the raw samples. This is the step that makes
        //    a client-reported fidelity worthless.
        TraceEvaluator evaluator = new TraceEvaluator();
        Map<GlyphInstance, TraceResult> traces = new HashMap<>();
        for (int i = 0; i < placements.size(); i++) {
            GlyphInstance placement = placements.get(i);
            Glyph glyph = glyphs.get(placement.glyphId()).orElse(null);
            if (glyph == null) {
                return;
            }

            List<Vec2> points;
            try {
                points = StrokeQuantizer.decode(payload.traces().get(i));
            } catch (IllegalArgumentException malformed) {
                return;
            }

            List<StrokePath> ideal = GlyphTransform.toCanvas(glyph, placement);
            float tolerance = GlyphTransform.toleranceFor(glyph, placement);
            TraceResult result = evaluator.evaluate(ideal, points, tolerance);

            if (!result.valid()) {
                return;
            }
            if (glyph.role() == GlyphRole.RING && !RingGeometry.isClosed(points, tolerance)) {
                return;
            }
            traces.put(placement, result);
        }

        // 8. Compile. Structural rules run a third time inside here; that's fine.
        SpellGraph graph = SpellGraphBuilder.build(placements, glyphs);
        CompileResult compiled = new SpellCompiler(glyphs).compile(graph, traces);
        if (!(compiled instanceof CompileResult.Success success)) {
            return;
        }

        // 9. Charge for it, and write it down — with the tools' character folded
        //    into the fidelity, once, here, because the pen won't be in anyone's
        //    hand when this parchment is finally cast.
        if (!InkSupply.spend(registries, menu.ink(), cost)) {
            return;
        }

        ItemStack parchment = menu.parchment();
        ItemStack inscribed = parchment.copyWithCount(1);
        parchment.shrink(1);

        CompiledSpell spell = DraftQuality.stamp(
                success.spell(), context.pen(), context.parchmentQuality());

        inscribed.set(SigilsComponents.SPELL.get(), spell);

        // What drew it, remembered on the sheet. The ink grade is a property of
        // the table at this moment; the parchment may be bound days later.
        context.inkGrade().ifPresent(grade ->
                inscribed.set(SigilsComponents.INK_GRADE.get(), grade.id()));

        // How far this pen can lay out a circle. Absent on a feather-drawn sheet,
        // which is how a single-cell sigil stays the default with no branch here.
        int reach = context.pen().maxWorldSigilRadius();
        if (reach > 0) {
            inscribed.set(SigilsComponents.SIGIL_RADIUS.get(), reach);
        }

        if (parchment.isEmpty()) {
            menu.setParchment(inscribed);
        } else if (!serverPlayer.getInventory().add(inscribed)) {
            serverPlayer.drop(inscribed, false);
        }

        menu.markChanged();
        menu.broadcastChanges();
    }

    private static boolean finite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}