package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

import com.sigils.Sigils;
import com.sigils.core.sigil.SigilIntegrity;
import com.sigils.registry.SigilsInks;

/**
 * Rain takes impermanent ink off anything left under the open sky.
 *
 * <p>One listener for the whole server rather than a ticker on every sigil. The
 * cost when it isn't raining is one boolean per level per tick; the cost when it
 * is raining is one hash per indexed chunk, and real work only for the chunks
 * whose turn it is.
 */
@EventBusSubscriber(modid = Sigils.MOD_ID)
public final class SigilWeather {

    /**
     * Ticks between weather passes for any one chunk.
     *
     * <p>At 200 ticks and {@link SigilIntegrity#WEATHER_STEP} of 0.02, a sigil in
     * the open goes from fresh to inert in about eight minutes of continuous rain
     * — most of one storm. That is meant to hurt: it's the reason netherite ink is
     * worth making, and the reason a roof is worth building.
     */
    public static final int WEATHER_INTERVAL = 200;

    private SigilWeather() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.isRaining()) {
                continue; // the overwhelmingly common case, and it costs a boolean
            }
            weather(level);
        }
    }

    private static void weather(ServerLevel level) {
        SigilIndex index = SigilIndex.of(level);
        if (index.isEmpty()) {
            return;
        }
        List<BlockPos> due = index.due(level.getGameTime(), WEATHER_INTERVAL);
        if (due.isEmpty()) {
            return;
        }

        RegistryAccess registries = level.registryAccess();
        for (BlockPos pos : due) {
            // isRainingAt is the whole "uncovered outdoor" test: it checks the
            // weather, the sky above this exact block, and whether the biome rains
            // at all. A sigil under a roof, underground, or in a desert is safe,
            // and none of that needed writing.
            if (!level.isRainingAt(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil)) {
                continue;
            }
            SigilIntegrity integrity = sigil.integrity();
            if (integrity.inert()) {
                continue; // there's nothing left to take
            }
            sigil.setIntegrity(integrity.weathered(
                    SigilsInks.isPermanent(registries, sigil.inkGradeId())));
        }
    }
}