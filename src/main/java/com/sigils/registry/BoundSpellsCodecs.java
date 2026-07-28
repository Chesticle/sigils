package com.sigils.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.sigils.core.spell.BoundSpells;

/** Disk and network form of {@link BoundSpells}, beside {@link CompiledSpellCodecs}. */
public final class BoundSpellsCodecs {

    private BoundSpellsCodecs() {}

    public static final Codec<BoundSpells> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompiledSpellCodecs.CODEC.listOf().fieldOf("spells")
                    .forGetter(BoundSpells::spells),
            Codec.intRange(0, BoundSpells.MAX_PAGES - 1).optionalFieldOf("active", 0)
                    .forGetter(BoundSpells::active)
    ).apply(instance, BoundSpells::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoundSpells> STREAM_CODEC =
            StreamCodec.composite(
                    // The list cap is the reader's, not the writer's: a hand-made
                    // packet claiming 40,000 pages is rejected before allocation.
                    CompiledSpellCodecs.STREAM_CODEC.apply(
                            ByteBufCodecs.list(BoundSpells.MAX_PAGES)), BoundSpells::spells,
                    ByteBufCodecs.VAR_INT, BoundSpells::active,
                    BoundSpells::new);
}