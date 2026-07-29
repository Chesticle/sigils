package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.sigils.core.sigil.PollSchedule;

/**
 * Where the loaded sigils are, per level, bucketed by chunk.
 *
 * <p>The point is to be able to answer "which sigils are near here?" and "whose
 * turn is it to be rained on?" without walking every block entity in every loaded
 * chunk. Weather in Part C uses the second; Part E's multiblock footprints use
 * the first.
 *
 * <p>One instance per {@link ServerLevel}, held weakly, on the same reasoning as
 * {@code RegistryCache}: a server has several levels, they come and go, and a
 * static field for "the" index would be wrong in the Nether.
 *
 * <p><b>Contains only loaded sigils.</b> Entries arrive in {@code onLoad} and
 * leave in {@code setRemoved}, so an unloaded chunk's sigils are simply absent —
 * which is correct, because nothing should be happening to them.
 */
public final class SigilIndex {

    private static final Map<ServerLevel, SigilIndex> BY_LEVEL =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<ChunkPos, Set<BlockPos>> byChunk = new HashMap<>();

    private SigilIndex() {}

    /**
     * The chunk containing a block.
     *
     * <p>{@code >> 4} rather than {@code / 16}, because the shift floors and the
     * divide truncates: block x = -1 is in chunk -1, and integer division would
     * say chunk 0. Every sigil west or north of the origin would land in the wrong
     * bucket, which is the sort of bug that only shows up in one quadrant of one
     * world and gets reported as "the index randomly forgets things".
     */
    private static ChunkPos chunkOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static SigilIndex of(ServerLevel level) {
        return BY_LEVEL.computeIfAbsent(level, ignored -> new SigilIndex());
    }

    public synchronized void add(BlockPos pos) {
        byChunk.computeIfAbsent(chunkOf(pos), ignored -> new HashSet<>())
                .add(pos.immutable());
    }

    public synchronized void remove(BlockPos pos) {
        ChunkPos chunk = chunkOf(pos);
        Set<BlockPos> inChunk = byChunk.get(chunk);
        if (inChunk == null) {
            return;
        }
        inChunk.remove(pos);
        if (inChunk.isEmpty()) {
            byChunk.remove(chunk); // don't accumulate empty buckets for visited chunks
        }
    }

    public synchronized boolean isEmpty() {
        return byChunk.isEmpty();
    }

    public synchronized int size() {
        return byChunk.values().stream().mapToInt(Set::size).sum();
    }

    public synchronized int chunkCount() {
        return byChunk.size();
    }

    /**
     * Positions in the chunks whose turn comes up this tick.
     *
     * <p>Staggered per chunk rather than per sigil, so a hundred sigils in one
     * chunk are one batch of work rather than a hundred scattered ones — the
     * opposite of what you want for polling triggers, and the right thing for a
     * pass that walks a whole chunk's worth at once.
     *
     * <p>Returns a copy, so callers may change the world while iterating.
     */
    public synchronized List<BlockPos> due(long gameTime, int intervalTicks) {
        if (byChunk.isEmpty()) {
            return List.of();
        }
        List<BlockPos> due = new ArrayList<>();
        for (Map.Entry<ChunkPos, Set<BlockPos>> entry : byChunk.entrySet()) {
            if (PollSchedule.due(gameTime, entry.getKey().hashCode(), intervalTicks)) {
                due.addAll(entry.getValue());
            }
        }
        return due;
    }

    /** Every indexed sigil within {@code radius} blocks. Part E's footprint query. */
    public synchronized List<BlockPos> within(BlockPos centre, int radius) {
        List<BlockPos> found = new ArrayList<>();
        int chunkRadius = (radius >> 4) + 1;
        ChunkPos origin = chunkOf(centre);
        long radiusSquared = (long) radius * radius;

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                Set<BlockPos> inChunk = byChunk.get(new ChunkPos(origin.x() + x, origin.z() + z));
                if (inChunk == null) {
                    continue;
                }
                for (BlockPos pos : inChunk) {
                    if (pos.distSqr(centre) <= radiusSquared) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }
}