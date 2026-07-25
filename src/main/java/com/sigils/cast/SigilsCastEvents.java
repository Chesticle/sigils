package com.sigils.cast;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Game-bus listeners for the casting system. */
public final class SigilsCastEvents {

    private SigilsCastEvents() {}

    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        SpellCasting.resetTickBudget();
    }
}