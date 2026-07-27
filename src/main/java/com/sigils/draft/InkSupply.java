package com.sigils.draft;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

import com.sigils.item.SigilsItems;

/** How much ink each item is worth. Same table-not-switch rule as {@link PenTiers}. */
public final class InkSupply {

    /** Units of ink in one item. A crest costs ~2 at full scale, so this is a few glyphs. */
    public static final float UNITS_PER_ITEM = 4f;

    private static Map<Item, Float> table;

    private InkSupply() {}

    private static Map<Item, Float> table() {
        if (table == null) {
            table = Map.of(SigilsItems.MAGICAL_INK.get(), UNITS_PER_ITEM);
        }
        return table;
    }

    /** Total ink in a stack: per-item value times count. */
    public static float capacityOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0f;
        }
        Float perItem = table().get(stack.getItem());
        return perItem == null ? 0f : perItem * stack.getCount();
    }

    public static boolean isInk(ItemStack stack) {
        return capacityOf(stack) > 0f;
    }

    /** How many whole items must be consumed to pay {@code cost}. */
    public static int itemsToConsume(ItemStack stack, float cost) {
        if (cost <= 0f || stack.isEmpty()) {
            return 0;
        }
        Float perItem = table().get(stack.getItem());
        if (perItem == null || perItem <= 0f) {
            return 0;
        }
        return (int) Math.ceil(cost / perItem);
    }
}