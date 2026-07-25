package com.sigils.cast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Resolves a delivery target id to a world point. */
public final class Targeting {

    private static final double REACH = 20.0;

    private Targeting() {}

    public static Vec3 resolve(CastContext ctx, String targetId) {
        ServerPlayer caster = ctx.caster();
        return switch (targetId) {
            case "sigils:looked_at_block" -> caster != null ? lookedAt(caster) : ctx.origin();
            case "sigils:self" -> caster != null ? caster.position() : ctx.origin();
            default -> ctx.origin();
        };
    }

    private static Vec3 lookedAt(ServerPlayer caster) {
        HitResult hit = caster.pick(REACH, 1.0f, false);
        return hit.getLocation();
    }
}