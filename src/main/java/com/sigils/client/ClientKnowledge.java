package com.sigils.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import com.sigils.Sigils;
import com.sigils.core.knowledge.KnownGlyphs;
import com.sigils.net.KnowledgePayload;

/**
 * The client's copy of what it may draw. A mirror, not a participant.
 *
 * <p>Nothing here decides anything. The server has already merged the innate
 * tag in, already refused whatever it was going to refuse, and will refuse it
 * again when a draft arrives. This exists so the palette can grey a cell out
 * without asking anyone.
 */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class ClientKnowledge {

    private static volatile KnownGlyphs known = KnownGlyphs.NONE;

    private ClientKnowledge() {}

    public static KnownGlyphs get() {
        return known;
    }

    /** Called from the packet handler, on the client thread. */
    public static void accept(KnowledgePayload payload) {
        known = payload.known();
    }

    /**
     * Forget everything on disconnect.
     *
     * <p>Without this, leaving a world where you knew everything and joining one
     * where you know nothing shows a fully unlocked palette until the join
     * packet lands — a window of one or two ticks in single-player and rather
     * longer on a bad connection. Nothing can be *inscribed* through that
     * window, because the server checks again, but a palette that flickers open
     * and then shut looks exactly like a bug.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        known = KnownGlyphs.NONE;
    }
}