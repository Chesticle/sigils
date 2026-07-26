package com.sigils.client;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Where particles spawn and their initial velocity, per delivery shape. */
public final class EmitterShapes {

    /** One particle to spawn: a position and an initial velocity. */
    public record Spawn(Vec3 pos, Vec3 vel) {}

    private EmitterShapes() {}

    public static List<Spawn> sample(String shapeId, Vec3 origin, Vec3 target,
                                     float scale, float speed, int count, RandomSource rng) {
        return switch (shapeId) {
            case "sigils:beam" -> beam(origin, target, speed, count, rng);
            case "sigils:aura" -> aura(origin, Math.max(0.5f, scale), speed, count, rng);
            default            -> burst(target, Math.max(0.25f, scale), speed, count, rng); // burst + fallback
        };
    }

    private static List<Spawn> beam(Vec3 from, Vec3 to, float speed, int count, RandomSource rng) {
        List<Spawn> out = new ArrayList<>(count);
        Vec3 along = to.subtract(from);
        double len = along.length();
        Vec3 unit = len < 1e-4 ? new Vec3(0, 1, 0) : along.scale(1.0 / len);
        for (int i = 0; i < count; i++) {
            Vec3 p = from.add(along.scale(rng.nextDouble()));
            Vec3 jitter = new Vec3(gauss(rng), gauss(rng), gauss(rng)).scale(0.05);
            out.add(new Spawn(p.add(jitter), unit.scale(speed)));
        }
        return out;
    }

    private static List<Spawn> burst(Vec3 centre, float radius, float speed, int count, RandomSource rng) {
        List<Spawn> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vec3 dir = randomUnit(rng);
            Vec3 p = centre.add(dir.scale(radius * rng.nextDouble()));
            out.add(new Spawn(p, dir.scale(speed)));
        }
        return out;
    }

    private static List<Spawn> aura(Vec3 centre, float radius, float speed, int count, RandomSource rng) {
        List<Spawn> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            Vec3 ring = new Vec3(Math.cos(a) * radius, 0.1 * gauss(rng), Math.sin(a) * radius);
            Vec3 outward = new Vec3(Math.cos(a), 0, Math.sin(a));
            out.add(new Spawn(centre.add(ring), outward.scale(speed)));
        }
        return out;
    }

    /** Uniform point on the unit sphere. */
    private static Vec3 randomUnit(RandomSource rng) {
        double z = rng.nextDouble() * 2 - 1;
        double a = rng.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(Math.max(0, 1 - z * z));
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }

    /** Cheap zero-mean noise in [-1, 1]. */
    private static double gauss(RandomSource rng) {
        return rng.nextDouble() - rng.nextDouble();
    }
}