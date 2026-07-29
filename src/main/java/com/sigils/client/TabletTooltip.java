package com.sigils.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.sigils.Sigils;
import com.sigils.item.GlyphTabletItem;

/**
 * Adds the personal half of a glyph tablet's tooltip.
 *
 * <p>Client-only by construction rather than by argument: this class is never
 * loaded on a dedicated server, so it may read {@link ClientKnowledge} freely.
 *
 * <p>Everything here is decoration. The server checks knowledge again when the
 * tablet is used, and refuses again if it has to.
 */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class TabletTooltip {

    private TabletTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof GlyphTabletItem)) {
            return;
        }
        GlyphTabletItem.glyphOf(event.getItemStack()).ifPresent(glyphId -> {
            boolean known = ClientKnowledge.get().knows(glyphId);
            event.getToolTip().add(Component.translatable(known
                            ? "tooltip.sigils.tablet.already_known"
                            : "tooltip.sigils.tablet.new")
                    .withStyle(known ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN));
        });
    }
}