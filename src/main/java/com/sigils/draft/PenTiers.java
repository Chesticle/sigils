package com.sigils.draft;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

import com.sigils.core.draft.DraftLimits;
import com.sigils.item.SigilsItems;

/**
 * What each pen is capable of.
 *
 * <p>A lookup table, deliberately — Phase 5 replaces the contents of {@link
 * #table()} with a datapack-loaded map and nothing else in the mod changes.
 * Every caller already asks the table rather than asking the item.
 */
public final class PenTiers {

    private static Map<Item, DraftLimits> table;

    private PenTiers() {}

    private static Map<Item, DraftLimits> table() {
        if (table == null) {
            // Built lazily: items don't exist yet when this class is first loaded.
            table = Map.of(SigilsItems.PEN.get(), DraftLimits.DRAFTING_TABLE);
        }
        return table;
    }

    public static Optional<DraftLimits> limitsFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table().get(stack.getItem()));
    }

    public static boolean isPen(ItemStack stack) {
        return limitsFor(stack).isPresent();
    }
}