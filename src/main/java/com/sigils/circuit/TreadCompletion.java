package com.sigils.circuit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

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

    @Override
    public int pollInterval() {
        return 2; // fast enough to catch a sprint; ten times cheaper than every tick
    }
}