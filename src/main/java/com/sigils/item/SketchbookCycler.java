package com.sigils.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.sigils.core.spell.BoundSpells;
import com.sigils.net.CycleSpellPayload;
import com.sigils.registry.SigilsComponents;

/** Handles {@link CycleSpellPayload}. The server owns the selection. */
public final class SketchbookCycler {

    private SketchbookCycler() {}

    public static void accept(CycleSpellPayload payload, Player player) {
        if (!(player instanceof ServerPlayer caster)) {
            return;
        }
        ItemStack book = caster.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(book.getItem() instanceof SketchbookItem)) {
            return; // the client thought it was holding one; it isn't
        }

        BoundSpells bound = SketchbookItem.contentsOf(book);
        BoundSpells cycled = bound.cycled(payload.direction());
        if (cycled == bound) {
            return; // nothing to turn to
        }

        book.set(SigilsComponents.BOUND_SPELLS.get(), cycled);
        caster.level().playSound(null, caster.blockPosition(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 1.2f);
    }
}