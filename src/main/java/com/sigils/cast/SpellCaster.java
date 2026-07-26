package com.sigils.cast;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import net.neoforged.neoforge.network.PacketDistributor;

import com.sigils.core.reaction.PhenomenonResolver;
import com.sigils.core.reaction.ReactionRule;
import com.sigils.core.reaction.Resolution;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.core.particle.ParticleProfile;
import com.sigils.core.particle.ProfileLookup;
import com.sigils.core.particle.SpellVisuals;
import com.sigils.net.SigilEmitterPayload;
import com.sigils.registry.SigilsProfiles;

/** Runs a compiled spell: resolve → deliver → dispatch effects. */
public final class SpellCaster {

    private static final PhenomenonResolver RESOLVER = new PhenomenonResolver();

    private SpellCaster() {}

    public static void cast(CastContext ctx, CompiledSpell spell, List<ReactionRule> rules) {
        // Where does it land?
        Vec3 target = Targeting.resolve(ctx, spell.delivery().targetId());

        // What does the mixture become? (Phase 1 engine.)
        Resolution resolution = RESOLVER.resolve(spell.mixture(), rules);

        // Which points does the delivery touch? (Phase 2.)
        List<Vec3> points = DeliveryShapes.points(ctx, target, spell.delivery());

        // 1. WORLD INTERACTION — effect handlers, now visuals-free (§6.2).
        for (Vec3 point : points) {
            for (Map.Entry<String, Float> phenomenon : resolution.phenomena().entrySet()) {
                dispatch(ctx, point, phenomenon.getKey(), phenomenon.getValue());
            }
            for (Map.Entry<String, Float> element : resolution.residual().asMap().entrySet()) {
                dispatch(ctx, point, element.getKey(), element.getValue());
            }
        }

        // 2. VISUALS — one blended profile, one packet. (Phase 3.)
        ProfileLookup lookup = SigilsProfiles.lookup(ctx.level().registryAccess());
        SpellVisuals.blend(resolution, lookup).ifPresent(profile -> {
            // Optional: a cast sound whose pitch tracks how fiery the mixture is.
            float fireShare = spell.mixture().ratioOf("sigils:fire");
            float pitch = Math.clamp(0.7f + 0.6f * fireShare, 0.5f, 2.0f);
            ctx.level().playSound(
                    null, BlockPos.containing(target),
                    SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                    0.6f, pitch);
            ParticleProfile shown = profile.perturbed(spell.baseInstability());
            int duration = spell.delivery().durationTicks() > 0 ? spell.delivery().durationTicks() : 12;
            SigilEmitterPayload payload = new SigilEmitterPayload(
                    shown,
                    spell.delivery().shapeId(),
                    ctx.origin(),
                    target,
                    spell.delivery().scale(),
                    Math.max(6, duration));
            PacketDistributor.sendToPlayersNear(
                    ctx.level(), null,
                    ctx.origin().x, ctx.origin().y, ctx.origin().z, 64.0,
                    payload);
        });
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