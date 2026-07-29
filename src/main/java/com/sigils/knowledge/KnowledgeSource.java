package com.sigils.knowledge;

import net.minecraft.server.level.ServerPlayer;

/**
 * How a player finds out they've learned something.
 *
 * <p>Runs on the server, after the grant has already happened and been synced.
 * A source cannot refuse a grant, cannot alter it, and cannot see the rest of
 * the set — which is what keeps it to nine lines and keeps every one of them
 * addable without touching {@link SigilsKnowledge}.
 */
@FunctionalInterface
public interface KnowledgeSource {

    /**
     * @param glyphId the glyph just learned, fully qualified
     */
    void announce(ServerPlayer player, String glyphId);

    /** Says nothing at all. Commands, datapack grants, tests. */
    KnowledgeSource SILENT = (player, glyphId) -> {};
}