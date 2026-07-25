package com.sigils.cast;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
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
        ServerLevel level = ctx.level();
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5, at.z,
                count(strength, 24, 120), 0.5, 0.4, 0.5, 0.02);
        for (LivingEntity e : nearby(level, at, 1.5 * strength)) {
            e.setRemainingFireTicks(0); // steam smothers open flame
        }
    }

    private static void combustion(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z,
                count(strength, 16, 80), 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z,
                count(strength, 6, 30), 0.3, 0.3, 0.3, 0.01);
        for (LivingEntity e : nearby(level, at, 1.5 * strength)) {
            e.setRemainingFireTicks((int) (strength * 40));
        }
    }

    private static void rawFire(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z,
                count(strength, 10, 60), 0.2, 0.2, 0.2, 0.02);
        for (LivingEntity e : nearby(level, at, 1.0 * strength)) {
            e.setRemainingFireTicks((int) (strength * 20));
        }
    }

    private static void rawWater(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z,
                count(strength, 16, 80), 0.4, 0.2, 0.4, 0.1);
        level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y + 0.2, at.z,
                count(strength, 8, 40), 0.3, 0.1, 0.3, 0.01);
        for (LivingEntity e : nearby(level, at, 1.5 * strength)) {
            e.setRemainingFireTicks(0);
        }
    }

    private static void rawEarth(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                at.x, at.y + 0.2, at.z, count(strength, 20, 100), 0.4, 0.2, 0.4, 0.05);
    }

    private static void rawAir(CastContext ctx, Vec3 at, float strength) {
        ServerLevel level = ctx.level();
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.4, at.z,
                count(strength, 12, 60), 0.6, 0.3, 0.6, 0.08);
    }
}