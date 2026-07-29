package com.sigils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;

/**
 * Silverwood foliage.
 *
 * <p>{@link LeavesBlock} is abstract in 26.x: it wants a codec and a falling-leaf
 * particle, and it takes the particle rate as a constructor argument. Folding
 * that rate in here rather than exposing it leaves a {@code Properties}-only
 * constructor, which is what {@code registerBlock} and {@code simpleCodec} both
 * want.
 */
public class SilverwoodLeavesBlock extends LeavesBlock {

    public static final MapCodec<SilverwoodLeavesBlock> CODEC =
            simpleCodec(SilverwoodLeavesBlock::new);

    /** Per-tick chance of a leaf drifting off. Vanilla's decorative leaves sit near this. */
    private static final float LEAF_PARTICLE_CHANCE = 0.01f;

    public SilverwoodLeavesBlock(Properties properties) {
        super(LEAF_PARTICLE_CHANCE, properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.CHERRY_LEAVES);
    }
}