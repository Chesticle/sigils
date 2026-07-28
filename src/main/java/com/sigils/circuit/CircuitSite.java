package com.sigils.circuit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.sigils.core.sigil.PollSchedule;

/**
 * Where a sigil is, told to whatever is deciding whether its ring is closed.
 *
 * @param level  the level the sigil is in
 * @param origin the cell the sigil occupies — the air block, not the block it is
 *               drawn on
 * @param face   the direction the drawn surface points, away from its support. A
 *               sigil on a floor faces {@link Direction#UP}.
 * @param radius footprint radius in blocks; 0 is a single cell. Part E's
 *               multiblock sigils set this; nothing in Parts A–D reads it.
 */
public record CircuitSite(Level level, BlockPos origin, Direction face, int radius) {

    public CircuitSite(Level level, BlockPos origin, Direction face) {
        this(level, origin, face, 0);
    }

    /** The block the sigil is drawn on. */
    public BlockPos support() {
        return origin.relative(face.getOpposite());
    }

    /** The cell directly in front of the drawn surface — where a piston can reach. */
    public BlockPos front() {
        return origin.relative(face);
    }

    public long gameTime() {
        return level.getGameTime();
    }

    /** Whether this site's turn in a staggered poll rotation is now. */
    public boolean due(int intervalTicks) {
        return PollSchedule.due(gameTime(), origin.hashCode(), intervalTicks);
    }
}