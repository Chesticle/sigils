package com.sigils.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;

import java.util.Map;

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

    public static Map<Item, PenCapabilities> loadAll(RegistryAccess access) {
        return ItemBoundTable.load(access, SigilsRegistries.PEN_TIER,
                (id, definition) -> definition.toCore());
    }
}