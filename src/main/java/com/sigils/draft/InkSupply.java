package com.sigils.draft;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import com.sigils.core.draft.InkGrade;
import com.sigils.item.InkCharge;
import com.sigils.item.InkVialItem;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsInks;

/**
 * How much ink is available, from either shape ink comes in: loose items worth a
 * fixed amount each, or a vial carrying a charge.
 *
 * <p>Everything above this class asks for a number and gets one. Only {@link
 * #spend} needs to know the difference.
 */
public final class InkSupply {

    private InkSupply() {}

    private static InkCharge chargeIn(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(SigilsComponents.INK_CHARGE.get());
    }

    /** Which grade is loaded — from a vial's memory, or from the item itself. */
    public static Optional<InkGrade> gradeOf(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        InkCharge charge = chargeIn(stack);
        if (charge != null) {
            return SigilsInks.byId(registries, charge.grade());
        }
        return Optional.ofNullable(SigilsInks.table(registries).get(stack.getItem()));
    }

    /** Total ink available, in units. */
    public static float capacityOf(RegistryAccess registries, ItemStack stack) {
        InkCharge charge = chargeIn(stack);
        if (charge != null) {
            return charge.units();
        }
        return gradeOf(registries, stack)
                .map(grade -> grade.unitsPerItem() * stack.getCount())
                .orElse(0f);
    }

    /**
     * Whether this belongs in the ink slot. An empty vial does — it's ink
     * equipment, and refusing it would mean taking it out to fill it.
     */
    public static boolean isInk(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof InkVialItem) {
            return true;
        }
        return SigilsInks.table(registries).containsKey(stack.getItem());
    }

    /**
     * Pay {@code cost} out of this stack. Returns false and changes nothing if it
     * can't be paid in full — an inscription is never half-charged.
     */
    public static boolean spend(RegistryAccess registries, ItemStack stack, float cost) {
        if (cost <= 0f) {
            return true;
        }
        InkCharge charge = chargeIn(stack);
        if (charge != null) {
            if (charge.units() < cost) {
                return false;
            }
            stack.set(SigilsComponents.INK_CHARGE.get(), charge.withUnits(charge.units() - cost));
            return true;
        }

        InkGrade grade = SigilsInks.table(registries).get(stack.getItem());
        if (grade == null) {
            return false;
        }
        int items = grade.itemsFor(cost);
        if (items > stack.getCount()) {
            return false;
        }
        stack.shrink(items);
        return true;
    }
}