package com.sigils.draft;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import com.sigils.core.draft.InkGrade;
import com.sigils.registry.SigilsInks;

/** How much ink each item is worth. Same table-not-switch rule as {@link PenTiers}. */
public final class InkSupply {

    private InkSupply() {}

    public static Optional<InkGrade> gradeOf(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SigilsInks.table(registries).get(stack.getItem()));
    }

    /** Total ink in a stack: the grade's per-item value times the count. */
    public static float capacityOf(RegistryAccess registries, ItemStack stack) {
        return gradeOf(registries, stack)
                .map(grade -> grade.unitsPerItem() * stack.getCount())
                .orElse(0f);
    }

    public static boolean isInk(RegistryAccess registries, ItemStack stack) {
        return capacityOf(registries, stack) > 0f;
    }

    /** How many whole items must be consumed to pay {@code cost}. */
    public static int itemsToConsume(RegistryAccess registries, ItemStack stack, float cost) {
        return gradeOf(registries, stack).map(grade -> grade.itemsFor(cost)).orElse(0);
    }
}