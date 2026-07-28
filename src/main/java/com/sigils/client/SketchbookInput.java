package com.sigils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.sigils.Sigils;
import com.sigils.core.spell.BoundSpells;
import com.sigils.item.SketchbookItem;
import com.sigils.net.CycleSpellPayload;

/** Sneak-scroll with a sketchbook in hand turns the page instead of the hotbar. */
@EventBusSubscriber(modid = Sigils.MOD_ID, value = Dist.CLIENT)
public final class SketchbookInput {

    private SketchbookInput() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SketchbookItem)) {
            return;
        }

        BoundSpells bound = SketchbookItem.contentsOf(held);
        if (bound.size() < 2) {
            return; // let the hotbar have the scroll; there's nothing to turn to
        }

        double delta = event.getScrollDeltaY();
        if (delta == 0) {
            return;
        }
        ClientPacketDistributor.sendToServer(new CycleSpellPayload(delta > 0 ? 1 : -1));
        event.setCanceled(true);
    }
}