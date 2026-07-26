package com.sigils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sigils.net.SigilEmitterPayload;
import com.sigils.particle.SigilParticleOptions;

/**
 * Turns each emitter packet into a short-lived emitter that spawns SigilParticles
 * locally, under a hard per-tick budget and distance LOD. All client-thread only.
 */
public final class EmitterClient {

    private static final int MAX_PARTICLES_PER_TICK = 400; // hard ceiling across all emitters
    private static final int MAX_ACTIVE_EMITTERS = 128;
    private static final double CULL_DISTANCE = 64.0;      // beyond this, emit nothing

    private static final List<Emitter> ACTIVE = new ArrayList<>();
    private static final RandomSource RNG = RandomSource.create();

    private EmitterClient() {}

    /** Called on the client thread from the packet handler. */
    public static void accept(SigilEmitterPayload payload) {
        if (ACTIVE.size() < MAX_ACTIVE_EMITTERS) {
            ACTIVE.add(new Emitter(payload));
        }
    }

    /** Called once per client tick (see {@link EmitterClientEvents}). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            ACTIVE.clear();
            return;
        }
        Vec3 cam = mc.player.getEyePosition(); // viewpoint for distance LOD

        int spentThisTick = 0;
        Iterator<Emitter> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Emitter e = it.next();
            spentThisTick += e.tick(level, cam, MAX_PARTICLES_PER_TICK - spentThisTick);
            if (e.done()) {
                it.remove();
            }
        }
    }

    private static final class Emitter {
        private final SigilEmitterPayload p;
        private int age;

        Emitter(SigilEmitterPayload p) { this.p = p; }

        boolean done() { return age >= p.durationTicks(); }

        int tick(ClientLevel level, Vec3 cam, int budgetLeft) {
            age++;
            if (budgetLeft <= 0) {
                return 0;
            }
            double dist = cam.distanceTo(p.origin());
            if (dist > CULL_DISTANCE) {
                return 0; // too far to matter
            }
            // Distance LOD: full rate up close, thinning with range.
            float lod = (float) Math.clamp(1.0 - dist / CULL_DISTANCE, 0.1, 1.0);

            int base = Math.round(8f * p.profile().density() * lod);
            int count = Math.max(0, Math.min(base, budgetLeft));
            if (count == 0) {
                return 0;
            }

            float speed = p.profile().speed();
            SigilParticleOptions options = new SigilParticleOptions(p.profile());
            for (EmitterShapes.Spawn s : EmitterShapes.sample(
                    p.shapeId(), p.origin(), p.target(), p.scale(), speed, count, RNG)) {
                level.addParticle(options,
                        s.pos().x, s.pos().y, s.pos().z,
                        s.vel().x, s.vel().y, s.vel().z);
            }
            return count;
        }
    }
}