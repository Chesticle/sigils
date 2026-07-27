package com.sigils.draft;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.PenCapabilities;
import com.sigils.registry.SigilsPens;

/**
 * What each pen is capable of — now a datapack table.
 *
 * <p>Phase 4 built this map in code with one entry, precisely so that this
 * change would be confined to one file. Callers still ask the table rather than
 * asking the item; the table just reads JSON now.
 */
public final class PenTiers {

    private PenTiers() {}

    /** Everything this pen can do, or empty if the item isn't a pen at all. */
    public static Optional<PenCapabilities> capabilitiesFor(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SigilsPens.table(registries).get(stack.getItem()));
    }

    /** Just the canvas limits — what Phase 4's validator and palette already read. */
    public static Optional<DraftLimits> limitsFor(RegistryAccess registries, ItemStack stack) {
        return capabilitiesFor(registries, stack).map(PenCapabilities::limits);
    }

    public static boolean isPen(RegistryAccess registries, ItemStack stack) {
        return capabilitiesFor(registries, stack).isPresent();
    }
}