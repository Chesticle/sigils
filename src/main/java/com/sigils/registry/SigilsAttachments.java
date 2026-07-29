package com.sigils.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import com.sigils.Sigils;
import com.sigils.core.knowledge.KnownGlyphs;

/**
 * Per-player data that isn't an item and isn't a block.
 *
 * <p>Attachments are NeoForge's answer to "capabilities, but for saved data",
 * and this is the roadmap's stated mechanism for glyph knowledge.
 */
public final class SigilsAttachments {

    private SigilsAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Sigils.MOD_ID);

    /**
     * What this player has learned. <em>Learned only</em> — innate glyphs are
     * not stored here, because the innate tag can change with a datapack and a
     * copy baked into a save file the first time a player logged in would then
     * be wrong forever.
     */
    public static final Supplier<AttachmentType<KnownGlyphs>> KNOWN_GLYPHS =
            ATTACHMENTS.register("known_glyphs", () -> AttachmentType
                    .builder(() -> KnownGlyphs.NONE)
                    .serialize(KnownGlyphsCodecs.MAP_CODEC, known -> !known.isEmpty())
                    .copyOnDeath()
                    .build());

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}