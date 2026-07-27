package com.sigils.registry;

import net.minecraft.core.RegistryAccess;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * One derived snapshot per {@link RegistryAccess}, rebuilt when a different one
 * turns up and dropped when it's collected.
 *
 * <p>A map rather than a single field, because in single-player there are two:
 * the integrated server's, and the client's synced copy. They hold equal data
 * and are different objects, and a one-slot cache would thrash between them.
 *
 * <p>Datapack registries are rebuilt on world load, not on {@code /reload}, so a
 * new world load means a new access object means a fresh snapshot — which is
 * exactly the invalidation rule we want, for free.
 */
public final class RegistryCache<T> {

    private final Map<RegistryAccess, T> byAccess = Collections.synchronizedMap(new WeakHashMap<>());
    private final Function<RegistryAccess, T> loader;

    public RegistryCache(Function<RegistryAccess, T> loader) {
        this.loader = loader;
    }

    public T get(RegistryAccess access) {
        return byAccess.computeIfAbsent(access, loader);
    }
}