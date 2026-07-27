package com.sigils.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.sigils.draft.DraftInscriber;

import com.sigils.Sigils;

/** Registers the emitter packet. Common (both sides); the handler only runs on the client. */
@EventBusSubscriber(modid = Sigils.MOD_ID)
public final class SigilsNetwork {

    private SigilsNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1"); // network version string
        registrar.playToClient(
                SigilEmitterPayload.TYPE,
                SigilEmitterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> com.sigils.client.EmitterClient.accept(payload)));
        registrar.playToServer(
                SpellDraftPayload.TYPE,
                SpellDraftPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> DraftInscriber.accept(payload, context.player())));
    }
}