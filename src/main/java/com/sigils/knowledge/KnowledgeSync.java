package com.sigils.knowledge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.sigils.Sigils;

/** Pushes a player's knowledge to their client whenever their client is new. */
@EventBusSubscriber(modid = Sigils.MOD_ID)
public final class KnowledgeSync {

    private KnowledgeSync() {}

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    /**
     * A respawned player is a new entity with a fresh attachment copied across
     * by {@code copyOnDeath}, and the client rebuilds a good deal of state on
     * the death screen. One packet is cheaper than finding out which half of
     * that assumption is wrong.
     */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SigilsKnowledge.sync(serverPlayer);
        }
    }
}