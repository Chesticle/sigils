package com.sigils.client;

import com.sigils.block.SigilsBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import com.sigils.Sigils;
import com.sigils.client.particle.SigilParticle;
import com.sigils.client.screen.DraftingTableScreen;
import com.sigils.menu.SigilsMenus;
import com.sigils.particle.SigilsParticles;

import java.util.List;

/** Client-only mod-bus setup. */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class SigilsClient {

    private SigilsClient() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SigilsParticles.SIGIL.get(), SigilParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        // One source, at list position 0 — which is the "tintindex": 0 in the model.
        event.register(List.of(new SigilTintSource()), SigilsBlocks.WORLD_SIGIL.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SigilsMenus.DRAFTING_TABLE.get(), DraftingTableScreen::new);
    }
}