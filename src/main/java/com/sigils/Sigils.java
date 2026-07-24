package com.sigils;

import com.sigils.core.SigilsCore;
import com.sigils.registry.SigilsRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint.
 *
 * <p>Deliberately thin. Its only job is to wire subsystems onto the event buses.
 * All real logic lives in its own class so this file never becomes a dumping
 * ground.
 */
@Mod(Sigils.MOD_ID)
public final class Sigils {

    public static final String MOD_ID = "sigils";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sigils");

    public Sigils(IEventBus modBus, ModContainer container) {
        // Mod bus: registration, setup, datagen. Fired per-mod during loading.
        SigilsRegistries.register(modBus);

        // Game bus: gameplay events. Fired during play.
        NeoForge.EVENT_BUS.addListener(com.sigils.command.SigilsCommands::onRegisterCommands);

        container.registerConfig(ModConfig.Type.COMMON, SigilsConfig.SPEC);

        LOGGER.info("Sigils loading — spell schema v{}", SigilsCore.SPELL_SCHEMA_VERSION);
    }

    /**
     * Shorthand for building an id in our namespace.
     *
     * <p>Use this everywhere instead of writing the namespace by hand. Typos in
     * namespaces produce silent failures rather than compile errors.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}