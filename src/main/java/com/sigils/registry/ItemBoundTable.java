package com.sigils.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.sigils.Sigils;

/**
 * Builds an {@code Item -> value} table from any datapack registry whose entries
 * name an item.
 *
 * <p>Pen tiers, ink grades and parchment grades all have the same shape: a file
 * named after the tier, binding an item, carrying some numbers. This is the one
 * place that turns a registry of those into a lookup, so the rules about
 * ordering, unknown items and duplicates are stated once.
 */
public final class ItemBoundTable {

    private ItemBoundTable() {}

    /** A datapack definition that binds itself to an item. */
    public interface Bound {
        Identifier item();
    }

    public static <D extends Bound, V> Map<Item, V> load(
            RegistryAccess access,
            ResourceKey<Registry<D>> registryKey,
            BiFunction<Identifier, D, V> toValue) {

        Registry<D> registry = access.lookupOrThrow(registryKey);

        // Sorted, so which entry wins a contested item doesn't depend on load order.
        List<Identifier> ids = new ArrayList<>(registry.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));

        Map<Item, V> table = new HashMap<>();
        for (Identifier id : ids) {
            D definition = registry.getValue(id);
            if (definition == null) {
                continue;
            }
            Identifier itemId = definition.item();

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                // Normal for a pack written against a mod that isn't installed.
                Sigils.LOGGER.debug("{} binds unknown item {} — skipped", id, itemId);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item == null) {
                continue;
            }
            if (table.containsKey(item)) {
                Sigils.LOGGER.warn("{} also binds {}, which is already claimed — skipped", id, itemId);
                continue;
            }
            table.put(item, toValue.apply(id, definition));
        }
        return Map.copyOf(table);
    }
}