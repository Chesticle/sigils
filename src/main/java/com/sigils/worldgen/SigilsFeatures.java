package com.sigils.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import com.sigils.Sigils;

/**
 * Keys for worldgen content that lives in datapack JSON.
 *
 * <p>A {@link ResourceKey} is a name, not a value — it resolves nothing and
 * loads nothing, which is exactly why a block registered during mod
 * construction may hold one for a file that won't be read until a world opens.
 */
public final class SigilsFeatures {

    private SigilsFeatures() {}

    /** The tree itself. Defined in {@code worldgen/configured_feature/silverwood_tree.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Sigils.id("silverwood_tree"));
}