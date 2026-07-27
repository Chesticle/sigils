package com.sigils.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sigils.Sigils;
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
        Registry<InkGradeDefinition> registry = access.lookupOrThrow(SigilsRegistries.INK_GRADE);

        List<Identifier> gradeIds = new ArrayList<>(registry.keySet());
        gradeIds.sort(Comparator.comparing(Identifier::toString));

        Map<Item, InkGrade> table = new HashMap<>();
        for (Identifier gradeId : gradeIds) {
            InkGradeDefinition definition = registry.getValue(gradeId);
            if (definition == null) {
                continue;
            }
            Identifier itemId = definition.item();

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                Sigils.LOGGER.debug("Ink grade {} binds unknown item {} — skipped", gradeId, itemId);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item == null) {
                continue;
            }
            if (table.containsKey(item)) {
                Sigils.LOGGER.warn("Ink grade {} also binds {}, which is already claimed — skipped",
                        gradeId, itemId);
                continue;
            }
            table.put(item, definition.toCore(gradeId));
        }
        return Map.copyOf(table);
    }
}