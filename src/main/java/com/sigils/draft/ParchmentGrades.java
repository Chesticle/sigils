package com.sigils.draft;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import com.sigils.registry.SigilsParchments;

/** Which sheets may be drafted on, and how well they hold a line. */
public final class ParchmentGrades {

    private ParchmentGrades() {}

    public static Optional<Float> qualityOf(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SigilsParchments.table(registries).get(stack.getItem()));
    }

    public static boolean isParchment(RegistryAccess registries, ItemStack stack) {
        return qualityOf(registries, stack).isPresent();
    }
}