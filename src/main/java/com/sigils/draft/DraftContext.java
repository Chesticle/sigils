package com.sigils.draft;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.InkGrade;
import com.sigils.core.draft.PenCapabilities;
import com.sigils.item.SigilsItems;

/**
 * The tools in the table, read as drafting rules.
 *
 * <p>Both sides build this from the same three item stacks and the same synced
 * registries, so the screen's idea of what's allowed and the server's cannot
 * drift. The server still re-derives it when a draft arrives.
 *
 * @param pen         what the pen in the slot can do
 * @param inkGrade    which ink is loaded, if any
 * @param inkCapacity total ink available, in units
 * @param missing     what the player still needs, phrased for display
 */
public record DraftContext(
        PenCapabilities pen,
        Optional<InkGrade> inkGrade,
        float inkCapacity,
        List<String> missing
) {
    public DraftContext {
        missing = List.copyOf(missing);
    }

    /** What the canvas will accept. Unchanged from Phase 4's point of view. */
    public DraftLimits limits() {
        return pen.limits();
    }

    public boolean ready() {
        return missing.isEmpty();
    }

    /** ARGB, ready to draw with — the grade's tint is authored without alpha. */
    public int inkTint() {
        return 0xFF000000 | inkGrade.map(InkGrade::tint).orElse(0x2A2440);
    }

    /** Whether the loaded ink survives washing and may be bound into a sketchbook. */
    public boolean permanentInk() {
        return inkGrade.map(InkGrade::permanent).orElse(false);
    }

    /**
     * How faithfully the paper holds a traced line. Part B reads this from a
     * parchment grade table; until then every sheet is neutral.
     */
    public float parchmentQuality() {
        return 1f;
    }

    public static DraftContext of(RegistryAccess registries,
                                  ItemStack parchment, ItemStack pen, ItemStack ink) {
        List<String> missing = new ArrayList<>();

        if (!parchment.is(SigilsItems.PARCHMENT.get())) {
            missing.add("parchment");
        }
        PenCapabilities capabilities = PenTiers.capabilitiesFor(registries, pen).orElse(null);
        if (capabilities == null) {
            missing.add("a pen");
        }
        Optional<InkGrade> grade = InkSupply.gradeOf(registries, ink);
        float capacity = InkSupply.capacityOf(registries, ink);
        if (capacity <= 0f) {
            missing.add("ink");
        }

        // With no pen we still hand back the table's limits so the screen has a
        // canvas radius to draw. ready() is false, so nothing can be confirmed.
        return new DraftContext(
                capabilities == null ? PenCapabilities.plain(DraftLimits.DRAFTING_TABLE) : capabilities,
                grade,
                capacity,
                missing);
    }
}