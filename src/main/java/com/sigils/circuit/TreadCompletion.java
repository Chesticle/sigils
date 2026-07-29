package com.sigils.circuit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Closed while a living thing is standing in the sigil.
 *
 * <p>The jump pad without the pressure plate, and the only one of the four that
 * genuinely has to be polled: entities move without producing block updates.
 */
public final class TreadCompletion implements CircuitCompletion {

    @Override
    public boolean isClosed(CircuitSite site) {
        BlockPos pos = site.origin();
        AABB cell = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        return !site.level()
                .getEntitiesOfClass(LivingEntity.class, cell, LivingEntity::isAlive)
                .isEmpty();
    }

    /**
     * One entity query for the whole ring instead of one per cell.
     *
     * <p>This is the roadmap's TPS warning, answered. A radius-16 circle is about
     * a hundred cells; polled every two ticks, the default loop would be fifty
     * bounding-box queries a tick for one sigil. One query over the ring's bounds
     * plus a containment test is the same answer for a fraction of the cost, and
     * the usual case — nobody standing anywhere near it — exits after the query
     * finds nothing.
     */
    @Override
    public boolean isClosedAnywhere(Level level, List<BlockPos> footprint,
                                    Direction face, int radius) {
        if (footprint.size() < 4) {
            return CircuitCompletion.super.isClosedAnywhere(level, footprint, face, radius);
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos cell : footprint) {
            minX = Math.min(minX, cell.getX()); maxX = Math.max(maxX, cell.getX());
            minY = Math.min(minY, cell.getY()); maxY = Math.max(maxY, cell.getY());
            minZ = Math.min(minZ, cell.getZ()); maxZ = Math.max(maxZ, cell.getZ());
        }
        AABB bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

        List<LivingEntity> found =
                level.getEntitiesOfClass(LivingEntity.class, bounds, LivingEntity::isAlive);
        if (found.isEmpty()) {
            return false; // the overwhelmingly common case, and it costs one query
        }
        for (LivingEntity entity : found) {
            BlockPos at = entity.blockPosition();
            for (BlockPos cell : footprint) {
                if (cell.equals(at)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int pollInterval() {
        return 2; // fast enough to catch a sprint; ten times cheaper than every tick
    }
}