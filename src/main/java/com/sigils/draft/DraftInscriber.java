package com.sigils.draft;

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
import com.sigils.core.spell.CompileResult;
import com.sigils.core.spell.SpellCompiler;
import com.sigils.core.spell.SpellGraph;
import com.sigils.core.spell.SpellGraphBuilder;
import com.sigils.core.spell.ValidationResult;
import com.sigils.core.trace.TraceEvaluator;
import com.sigils.core.trace.TraceResult;
import com.sigils.item.SigilsItems;
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

        GlyphLookup glyphs = SigilsGlyphs.lookup(serverPlayer.level().registryAccess());

        // 5. Every arrangement rule, again.
        ValidationResult validation =
                DraftValidator.validate(placements, glyphs, context.limits());
        if (!validation.valid()) {
            Sigils.LOGGER.debug("Rejected draft from {}: {}",
                    serverPlayer.getName().getString(), validation.errors());
            return;
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

        // 9. Charge for it, and write it down.
        ItemStack ink = menu.ink();
        int inkItems = InkSupply.itemsToConsume(ink, cost);
        if (inkItems > ink.getCount()) {
            return;
        }
        ink.shrink(inkItems);

        ItemStack parchment = menu.parchment();
        parchment.shrink(1);

        ItemStack inscribed = new ItemStack(SigilsItems.PARCHMENT.get());
        inscribed.set(SigilsComponents.SPELL.get(), success.spell());

        if (parchment.isEmpty()) {
            menu.setParchment(inscribed);
        } else if (!serverPlayer.getInventory().add(inscribed)) {
            serverPlayer.drop(inscribed, false);
        }

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