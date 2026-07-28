package com.sigils.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Optional;

import com.sigils.core.draft.InkGrade;

/** The ink grade table: which item is worth what, built from datapack JSON. */
public final class SigilsInks {

    private static final RegistryCache<Map<Item, InkGrade>> CACHE =
            new RegistryCache<>(SigilsInks::loadAll);

    private SigilsInks() {}

    public static Map<Item, InkGrade> table(RegistryAccess access) {
        return CACHE.get(access);
    }

    public static Map<Item, InkGrade> loadAll(RegistryAccess access) {
        return ItemBoundTable.load(access, SigilsRegistries.INK_GRADE,
                (id, definition) -> definition.toCore(id));
    }

    /**
     * A grade by its own registry id rather than by item — what a vial needs,
     * since a vial's contents are a grade it remembers, not the item it is.
     */
    public static Optional<InkGrade> byId(RegistryAccess access, String gradeId) {
        for (InkGrade grade : table(access).values()) {
            if (grade.id().equals(gradeId)) {
                return Optional.of(grade);
            }
        }
        return Optional.empty();
    }
}