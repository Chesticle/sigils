package com.sigils.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import com.sigils.core.cast.CastGuard;

/** Server-wide entry point for beginning casts, backed by one shared guard. */
public final class SpellCasting {

    private static final CastGuard GUARD = CastGuard.standard();

    private SpellCasting() {}

    public static Optional<CastContext> begin(ServerLevel level, ServerPlayer caster, Vec3 origin) {
        return CastContext.begin(level, caster, origin, GUARD);
    }

    /** Reset the per-tick cast budget. Wired to the server tick below. */
    public static void resetTickBudget() {
        GUARD.resetTick();
    }
}