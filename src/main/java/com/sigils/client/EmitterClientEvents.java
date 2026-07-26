package com.sigils.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.sigils.Sigils;

/** Drives the emitter manager once per client tick. */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class EmitterClientEvents {

    private EmitterClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        EmitterClient.tick();
    }
}