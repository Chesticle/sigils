package com.sigils.knowledge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

import com.sigils.Sigils;
import com.sigils.registry.SigilsGlyphs;

/**
 * Every way a glyph can arrive, by id.
 *
 * <p>If this ever needs to be a real registry — so a datapack could restrict
 * which sources may teach a restricted glyph, say — it becomes one behind these
 * same three methods and no caller changes.
 */
public final class KnowledgeSources {

    /** A command, a datapack grant, a test. No message, no sound. */
    public static final Identifier COMMAND = Sigils.id("command");

    /** Part B's glyph tablet. Registered here so Part B is one line and a file. */
    public static final Identifier TABLET = Sigils.id("tablet");

    private static final Map<Identifier, KnowledgeSource> BY_ID = new HashMap<>();

    private KnowledgeSources() {}

    /** Call once from the mod constructor. */
    public static void bootstrap() {
        register(COMMAND, KnowledgeSource.SILENT);
        register(TABLET, chat("message.sigils.learned.tablet",
                SoundEvents.PLAYER_LEVELUP, 1.4f));
        Sigils.LOGGER.info("Sigils: {} knowledge source(s) registered", BY_ID.size());
    }

    public static synchronized void register(Identifier id, KnowledgeSource source) {
        if (BY_ID.putIfAbsent(id, source) != null) {
            Sigils.LOGGER.warn("Knowledge source {} is already registered — ignoring the second", id);
        }
    }

    /**
     * The source for an id, or {@link KnowledgeSource#SILENT} for one nobody
     * registered.
     *
     * <p>Never null and never throws. A grant attributed to a source from a mod
     * that has since been uninstalled still teaches the glyph; it just does so
     * quietly.
     */
    public static KnowledgeSource get(Identifier id) {
        return BY_ID.getOrDefault(id, KnowledgeSource.SILENT);
    }

    /**
     * The common shape: one line of chat naming the glyph, and a sound.
     *
     * <p>{@code messageKey} takes the glyph's display name as its only argument,
     * so a translator can put it anywhere in the sentence — which is the whole
     * reason this is a lang key and not a concatenation.
     */
    public static KnowledgeSource chat(String messageKey, SoundEvent sound, float pitch) {
        return (player, glyphId) -> {
            player.sendSystemMessage(Component.translatable(
                    messageKey,
                    Component.translatable(SigilsGlyphs.nameKey(glyphId))
                            .withStyle(ChatFormatting.GOLD)));
            player.level().playSound(null, player.blockPosition(),
                    sound, SoundSource.PLAYERS, 0.7f, pitch);
        };
    }
}