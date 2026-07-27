package com.sigils.draft;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import com.sigils.core.draft.DraftLimits;
import com.sigils.item.SigilsItems;

/**
 * The tools in the table, read as drafting rules.
 *
 * <p>Both sides build this from the same three item stacks, so the screen's idea
 * of what's allowed and the server's idea cannot drift. The server still
 * re-derives it when a draft arrives — it never takes the client's word.
 *
 * @param limits      what this pen permits
 * @param inkCapacity total ink available, in units
 * @param missing     what the player still needs, phrased for display
 */
public record DraftContext(DraftLimits limits, float inkCapacity, List<String> missing) {

    public DraftContext {
        missing = List.copyOf(missing);
    }

    public boolean ready() {
        return missing.isEmpty();
    }

    public static DraftContext of(ItemStack parchment, ItemStack pen, ItemStack ink) {
        List<String> missing = new ArrayList<>();

        if (!parchment.is(SigilsItems.PARCHMENT.get())) {
            missing.add("parchment");
        }
        DraftLimits limits = PenTiers.limitsFor(pen).orElse(null);
        if (limits == null) {
            missing.add("a pen");
        }
        float capacity = InkSupply.capacityOf(ink);
        if (capacity <= 0f) {
            missing.add("ink");
        }

        // With no pen we still hand back the table's limits so the screen has a
        // canvas radius to draw. ready() is false, so nothing can be confirmed.
        return new DraftContext(
                limits == null ? DraftLimits.DRAFTING_TABLE : limits,
                capacity,
                missing);
    }
}