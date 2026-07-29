package com.sigils.cast;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sigils.Sigils;

/**
 * The registry of effect handlers, keyed by phenomenon id or residual-element id,
 * plus the built-in effects. All effects are server-side particles and light
 * world interaction — real particle presets arrive in Phase 3.
 */
public final class SigilsEffects {

    private static final Map<Identifier, EffectHandler> HANDLERS = new HashMap<>();

    private SigilsEffects() {}

    public static void register(Identifier id, EffectHandler handler) {
        HANDLERS.put(id, handler);
    }

    public static EffectHandler get(Identifier id) {
        return HANDLERS.get(id);
    }

    public static Set<Identifier> ids() {
        return HANDLERS.keySet();
    }

    /** Register the built-in effects. Call once at startup. */
    public static void bootstrap() {
        // Reaction phenomena.
        register(Sigils.id("steam"), SigilsEffects::steam);
        register(Sigils.id("combustion"), SigilsEffects::combustion);
        // Residual pure elements — the "leftover fire still burns" effects.
        register(Sigils.id("fire"), SigilsEffects::rawFire);
        register(Sigils.id("water"), SigilsEffects::rawWater);
        register(Sigils.id("earth"), SigilsEffects::rawEarth);
        register(Sigils.id("air"), SigilsEffects::rawAir);
    }

    /** Radius of an air burst, in blocks, before strength is added. */
    private static final double AIR_RADIUS = 2.0;

    /** Shove away from the burst, per unit of strength, at its centre. */
    private static final double AIR_PUSH = 0.60;

    /** Upward component, always applied — otherwise a shove just scrapes you along the floor. */
    private static final double AIR_LIFT = 0.50;

    /** Air that throws you also catches you. Ticks of slow falling after a launch. */
    private static final int AIR_CUSHION_TICKS = 60;

    // ---- shared helpers -------------------------------------------------------

    private static int count(float strength, int perUnit, int cap) {
        return Math.min(cap, Math.max(1, Math.round(strength * perUnit)));
    }

    private static List<LivingEntity> nearby(ServerLevel level, Vec3 at, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(
                at.x - radius, at.y - radius, at.z - radius,
                at.x + radius, at.y + radius, at.z + radius));
    }

    // ---- effects --------------------------------------------------------------

    private static void steam(CastContext ctx, Vec3 at, float strength) {
        for (LivingEntity e : nearby(ctx.level(), at, 1.5 * strength)) {
            e.setRemainingFireTicks(0); // steam smothers open flame
        }
    }

    private static void combustion(CastContext ctx, Vec3 at, float strength) {
        for (LivingEntity e : nearby(ctx.level(), at, 1.5 * strength)) {
            e.setRemainingFireTicks((int) (strength * 40));
        }
    }

    private static void rawFire(CastContext ctx, Vec3 at, float strength) {
        for (LivingEntity e : nearby(ctx.level(), at, 1.5 * strength)) {
            e.setRemainingFireTicks((int) (strength * 20));
        }
    }

    private static void rawWater(CastContext ctx, Vec3 at, float strength) {
        for (LivingEntity e : nearby(ctx.level(), at, 1.5 * strength)) {
            e.setRemainingFireTicks(0);
        }
    }

    private static void rawEarth(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                at.x, at.y + 0.2, at.z, count(strength, 20, 100), 0.4, 0.2, 0.4, 0.05);
    }

    /**
     * Air moves things.
     *
     * <p>The push is <em>away from the burst point</em> rather than in any fixed
     * direction, which is the only reason one implementation covers three
     * behaviours: a floor sigil launches whoever stands on it, a wall sigil shoves
     * them off it, and an air beam knocks them along its length. None of those is
     * special-cased, and none of them knows a sigil exists.
     */
    private static void rawAir(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        double radius = AIR_RADIUS + strength;

        for (LivingEntity entity : nearby(level, at, radius)) {
            // Bounding-box centre, not position(): position() is an entity's feet,
            // and a player standing on a floor sigil has their feet *below* the
            // burst point — which would push them down through the floor.
            Vec3 away = entity.getBoundingBox().getCenter().subtract(at);
            double distance = away.length();
            Vec3 direction = distance < 1.0e-4
                    ? new Vec3(0, 1, 0)          // dead centre: straight up
                    : away.scale(1 / distance);

            // Linear falloff, so standing on it is very different from standing beside it.
            double force = strength * (1.0 - Math.min(1.0, distance / radius));
            Vec3 push = direction.scale(force * AIR_PUSH).add(0, force * AIR_LIFT, 0);

            entity.push(push.x, push.y, push.z);
            entity.resetFallDistance();

            if (push.y > 0.25) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING, AIR_CUSHION_TICKS, 0, true, false));
            }

            // A velocity change on the server does not reach a player's client on
            // its own — the client owns player movement and will simply ignore it.
            // hurtMarked is the flag the entity tracker watches; the explicit
            // packet covers players, for whom the tracker isn't enough.
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
    }
}