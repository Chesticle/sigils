package com.sigils.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.function.Consumer;

import com.sigils.core.draft.InkGrade;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsInks;

/**
 * A stoppered flask of ink. Sits in the drafting table's ink slot and is drawn
 * down rather than consumed.
 *
 * <p>Use it in hand to decant loose ink from your pack into it. A vial holds one
 * grade at a time — you cannot top up magical ink with netherite.
 */
public class InkVialItem extends Item {

    /** Six magical inks' worth, or four netherite. */
    public static final float CAPACITY = 24f;

    private static final int BAR_COLOUR = 0xFF6E5AA8;

    public InkVialItem(Properties properties) {
        super(properties);
    }

    /** Units currently held, 0 for a fresh or drained vial. */
    public static float unitsIn(ItemStack stack) {
        InkCharge charge = stack.get(SigilsComponents.INK_CHARGE.get());
        return charge == null ? 0f : charge.units();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack vial = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        RegistryAccess registries = level.registryAccess();
        InkCharge charge = vial.get(SigilsComponents.INK_CHARGE.get());

        float units = charge == null ? 0f : charge.units();
        String held = charge == null ? null : charge.grade();
        if (units >= CAPACITY - 1e-4f) {
            return InteractionResult.PASS;
        }

        Map<Item, InkGrade> inks = SigilsInks.table(registries);
        Inventory inventory = player.getInventory();
        boolean drew = false;

        for (int slot = 0; slot < inventory.getContainerSize() && units < CAPACITY; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty()) {
                continue;
            }
            InkGrade grade = inks.get(candidate.getItem());
            if (grade == null || grade.unitsPerItem() <= 0f) {
                continue; // not ink — including the vial itself, which isn't in the table
            }
            if (held != null && !held.equals(grade.id())) {
                continue; // one grade per vial
            }

            int room = (int) Math.floor((CAPACITY - units) / grade.unitsPerItem());
            if (room <= 0) {
                break;
            }
            int taken = Math.min(room, candidate.getCount());
            candidate.shrink(taken);
            units += taken * grade.unitsPerItem();
            held = grade.id();
            drew = true;
        }

        if (!drew) {
            return InteractionResult.PASS;
        }

        vial.set(SigilsComponents.INK_CHARGE.get(), new InkCharge(held, units));
        level.playSound(null, player.blockPosition(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.1f);
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------- Presentation

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return unitsIn(stack) > 0f;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * Math.clamp(unitsIn(stack) / CAPACITY, 0f, 1f));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOUR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        InkCharge charge = stack.get(SigilsComponents.INK_CHARGE.get());
        if (charge == null || charge.units() <= 0f) {
            tooltip.accept(Component.translatable("tooltip.sigils.ink_vial.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // "sigils:magical" -> "ink_grade.sigils.magical", so grades localise
        // without the tooltip needing the registries to resolve one.
        tooltip.accept(Component.translatable("tooltip.sigils.ink_vial.filled",
                        Component.translatable("ink_grade." + charge.grade().replace(':', '.')),
                        String.format("%.1f", charge.units()),
                        String.format("%.0f", CAPACITY))
                .withStyle(ChatFormatting.AQUA));
    }
}