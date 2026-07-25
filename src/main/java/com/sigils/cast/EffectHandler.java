package com.sigils.cast;

import net.minecraft.world.phys.Vec3;

/** Turns a resolved phenomenon or residual element into something in the world. */
@FunctionalInterface
public interface EffectHandler {
    void apply(CastContext ctx, Vec3 at, float strength);
}