package com.sigils.client;

import com.sigils.client.screen.DraftingTableScreen;
import com.sigils.menu.SigilsMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import com.sigils.Sigils;
import com.sigils.client.particle.SigilParticle;
import com.sigils.particle.SigilsParticles;

/** Client-only mod-bus setup. */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class SigilsClient {

    private SigilsClient() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SigilsParticles.SIGIL.get(), SigilParticle.Provider::new);
    }
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SigilsMenus.DRAFTING_TABLE.get(), DraftingTableScreen::new);
    }
}