package com.sigils.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import com.sigils.core.cast.CastGuard;

/**
 * The live context of one cast: where it is, who cast it, how deep in a chain it
 * is, and the shared guard. {@code caster} may be null (a command block or the
 * console can cast).
 */
public final class CastContext {

    private final ServerLevel level;
    private final ServerPlayer caster; // may be null
    private final Vec3 origin;
    private final int depth;
    private final CastGuard guard;

    private CastContext(ServerLevel level, ServerPlayer caster, Vec3 origin, int depth, CastGuard guard) {
        this.level = level;
        this.caster = caster;
        this.origin = origin;
        this.depth = depth;
        this.guard = guard;
    }

    /** Begin a top-level cast (depth 0). Empty if this tick's global budget is spent. */
    public static Optional<CastContext> begin(ServerLevel level, ServerPlayer caster, Vec3 origin, CastGuard guard) {
        if (!guard.tryBeginCast()) {
            return Optional.empty();
        }
        return Optional.of(new CastContext(level, caster, origin, 0, guard));
    }

    /**
     * Derive a chained sub-cast (depth + 1). This is the hook chain spells will
     * use in a later phase — nothing in Phase 2 calls it yet. It exists now so the
     * depth limit and tick budget are enforced automatically the moment chaining
     * is added. Empty if the depth limit is hit or the tick budget is spent.
     */
    public Optional<CastContext> descend(Vec3 childOrigin) {
        if (!guard.withinDepth(depth + 1)) {
            return Optional.empty();
        }
        if (!guard.tryBeginCast()) {
            return Optional.empty();
        }
        return Optional.of(new CastContext(level, caster, childOrigin, depth + 1, guard));
    }

    public ServerLevel level() {
        return level;
    }

    /** May be null. */
    public ServerPlayer caster() {
        return caster;
    }

    public Vec3 origin() {
        return origin;
    }

    public int depth() {
        return depth;
    }
}