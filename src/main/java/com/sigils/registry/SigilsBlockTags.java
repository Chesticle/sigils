package com.sigils.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.sigils.Sigils;

/** Block tags this mod's rules read. */
public final class SigilsBlockTags {

    private SigilsBlockTags() {}

    /** Trunks a sap tap will stay on. Part E's nether tree joins by JSON. */
    public static final TagKey<Block> TAPPABLE =
            TagKey.create(Registries.BLOCK, Sigils.id("tappable"));
}