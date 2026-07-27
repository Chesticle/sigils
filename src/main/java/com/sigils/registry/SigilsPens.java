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
import com.sigils.core.draft.PenCapabilities;

/** The pen tier table: which item is which pen, built from datapack JSON. */
public final class SigilsPens {

    private static final RegistryCache<Map<Item, PenCapabilities>> CACHE =
            new RegistryCache<>(SigilsPens::loadAll);

    private SigilsPens() {}

    /** The cached table. This is what everything should call. */
    public static Map<Item, PenCapabilities> table(RegistryAccess access) {
        return CACHE.get(access);
    }

    /** Walks the registry and resolves every tier's item. Call through {@link #table}. */
    public static Map<Item, PenCapabilities> loadAll(RegistryAccess access) {
        Registry<PenTierDefinition> registry = access.lookupOrThrow(SigilsRegistries.PEN_TIER);

        // Sorted, so which tier wins a contested item doesn't depend on load order.
        List<Identifier> tierIds = new ArrayList<>(registry.keySet());
        tierIds.sort(Comparator.comparing(Identifier::toString));

        Map<Item, PenCapabilities> table = new HashMap<>();
        for (Identifier tierId : tierIds) {
            PenTierDefinition definition = registry.getValue(tierId);
            if (definition == null) {
                continue;
            }
            Identifier itemId = definition.item();

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                // Normal for a pack written against a mod that isn't installed.
                Sigils.LOGGER.debug("Pen tier {} binds unknown item {} — skipped", tierId, itemId);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item == null) {
                continue;
            }
            if (table.containsKey(item)) {
                Sigils.LOGGER.warn("Pen tier {} also binds {}, which is already claimed — skipped",
                        tierId, itemId);
                continue;
            }
            table.put(item, definition.toCore());
        }
        return Map.copyOf(table);
    }
}