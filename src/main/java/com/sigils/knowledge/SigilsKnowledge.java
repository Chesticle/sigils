package com.sigils.knowledge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;

import com.sigils.core.knowledge.KnownGlyphs;
import com.sigils.net.KnowledgePayload;
import com.sigils.registry.SigilsAttachments;
import com.sigils.registry.SigilsGlyphs;

/**
 * Server-side access to what a player knows.
 *
 * <p>The one place that writes the attachment. Every grant saves, syncs and
 * announces in that order, so a client can never be told about a glyph the
 * server hasn't stored.
 */
public final class SigilsKnowledge {

    private SigilsKnowledge() {}

    /** What this player has been taught. Does not include innate glyphs. */
    public static KnownGlyphs learned(ServerPlayer player) {
        return player.getData(SigilsAttachments.KNOWN_GLYPHS.get());
    }

    /**
     * Everything this player may draw: learned plus innate.
     *
     * <p>This is the set the palette and the inscriber ask. Nothing downstream
     * of here knows that "known" has two sources, which is why adding a third —
     * a config-granted set, a per-dimension set, whatever Phase 9 wants — is a
     * change to this method and to nothing else.
     */
    public static KnownGlyphs effective(ServerPlayer player) {
        return learned(player).and(SigilsGlyphs.innate(player.level().registryAccess()));
    }

    /**
     * Teach one glyph.
     *
     * @return false if they already knew it — nothing was saved, sent or said
     */
    public static boolean grant(ServerPlayer player, String glyphId, Identifier sourceId) {
        KnownGlyphs before = learned(player);
        KnownGlyphs after = before.learned(glyphId);
        if (after == before) {
            return false;
        }
        player.setData(SigilsAttachments.KNOWN_GLYPHS.get(), after);
        sync(player);
        KnowledgeSources.get(sourceId).announce(player, glyphId);
        // Part E fires the advancement criterion here.
        return true;
    }

    /**
     * Unteach one glyph.
     *
     * <p>Only reachable from the command today. It exists because a knowledge
     * system you cannot reverse is a knowledge system you cannot test, and
     * because an admin fixing a botched datapack shouldn't have to edit a
     * player's save file.
     *
     * @return false if they didn't know it
     */
    public static boolean revoke(ServerPlayer player, String glyphId) {
        KnownGlyphs before = learned(player);
        KnownGlyphs after = before.forgotten(glyphId);
        if (after == before) {
            return false;
        }
        player.setData(SigilsAttachments.KNOWN_GLYPHS.get(), after);
        sync(player);
        return true;
    }

    /** Send this player their effective set. Cheap; a few dozen short strings. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new KnowledgePayload(effective(player)));
    }
}