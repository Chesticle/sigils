package com.sigils.client;

import com.sigils.block.WorldSigilBlock;
import com.sigils.core.sigil.SigilIntegrity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.sigils.block.WorldSigilBlockEntity;
import com.sigils.core.sigil.SigilTint;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/**
 * Tint index 0 on the world sigil model: the ink's hue, dimmed by wear.
 *
 * <p>A class rather than a lambda because the colour depends on block entity
 * data, and the single abstract method — {@link #color(BlockState)} — is handed
 * nothing but a block state. The world-aware overload is a {@code default}, and
 * lambdas cannot override those.
 */
public final class SigilTintSource implements BlockTintSource {

    /**
     * No world, no position, no block entity. This is what item frames, model
     * previews and anything asking about the block in the abstract get.
     */
    @Override
    public int color(BlockState state) {
        return SigilTint.FALLBACK;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil)) {
            return SigilTint.FALLBACK;
        }
        int ink = sigil.inkTint();
        if (ink < 0) {
            return SigilTint.FALLBACK;
        }
        return SigilTint.decal(
                ink,
                SigilIntegrity.remainingAt(state.getValue(WorldSigilBlock.WEAR)),
                state.getValue(WorldSigilBlock.LIT));
    }

    /**
     * {@code LIT} now changes the colour, so the tint cache has to be told —
     * otherwise it may treat this block's colour as constant and reuse a value
     * computed for some other state.
     */
    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(WorldSigilBlock.LIT, WorldSigilBlock.WEAR);
    }
}