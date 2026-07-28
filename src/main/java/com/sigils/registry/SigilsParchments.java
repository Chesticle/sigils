package com.sigils.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;

import java.util.Map;

/** The parchment grade table: which sheet holds a line how well. */
public final class SigilsParchments {

    private static final RegistryCache<Map<Item, Float>> CACHE =
            new RegistryCache<>(SigilsParchments::loadAll);

    private SigilsParchments() {}

    public static Map<Item, Float> table(RegistryAccess access) {
        return CACHE.get(access);
    }

    public static Map<Item, Float> loadAll(RegistryAccess access) {
        return ItemBoundTable.load(access, SigilsRegistries.PARCHMENT_GRADE,
                (id, definition) -> definition.quality());
    }
}