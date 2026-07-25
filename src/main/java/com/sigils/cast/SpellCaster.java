package com.sigils.cast;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import com.sigils.core.reaction.PhenomenonResolver;
import com.sigils.core.reaction.ReactionRule;
import com.sigils.core.reaction.Resolution;
import com.sigils.core.spell.CompiledSpell;

/** Runs a compiled spell: resolve → deliver → dispatch effects. */
public final class SpellCaster {

    private static final PhenomenonResolver RESOLVER = new PhenomenonResolver();

    private SpellCaster() {}

    public static void cast(CastContext ctx, CompiledSpell spell, List<ReactionRule> rules) {
        // 1. Where does it land?
        Vec3 target = Targeting.resolve(ctx, spell.delivery().targetId());

        // 2. What does the mixture become? (Phase 1 engine.)
        Resolution resolution = RESOLVER.resolve(spell.mixture(), rules);

        // 3. Which points does the delivery touch?
        List<Vec3> points = DeliveryShapes.points(ctx, target, spell.delivery());

        // 4. Dispatch every phenomenon and every residual element at each point.
        for (Vec3 point : points) {
            for (Map.Entry<String, Float> phenomenon : resolution.phenomena().entrySet()) {
                dispatch(ctx, point, phenomenon.getKey(), phenomenon.getValue());
            }
            for (Map.Entry<String, Float> element : resolution.residual().asMap().entrySet()) {
                dispatch(ctx, point, element.getKey(), element.getValue());
            }
        }
    }

    private static void dispatch(CastContext ctx, Vec3 at, String effectId, float strength) {
        Identifier id = Identifier.tryParse(effectId);
        if (id == null) {
            return;
        }
        EffectHandler handler = SigilsEffects.get(id);
        if (handler != null) {
            handler.apply(ctx, at, strength);
        }
        // No handler for this id yet? Silently skip — a phenomenon can exist in
        // data before it has a visual, and that's fine.
    }
}