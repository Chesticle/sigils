package com.sigils.registry;

import com.sigils.core.spell.BoundSpells;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.item.InkCharge;
import com.mojang.serialization.Codec;

/** Data components the mod attaches to items. */
public final class SigilsComponents {

    private SigilsComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Sigils.MOD_ID);

    /** The spell inscribed on a parchment (later: on an artifact). */
    public static final Supplier<DataComponentType<CompiledSpell>> SPELL =
            COMPONENTS.registerComponentType("spell", builder -> builder
                    .persistent(CompiledSpellCodecs.CODEC)
                    .networkSynchronized(CompiledSpellCodecs.STREAM_CODEC));

    /** Which ink grade a spell was inscribed with — the sketchbook's gate. */
    public static final Supplier<DataComponentType<String>> INK_GRADE =
            COMPONENTS.registerComponentType("ink_grade", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.stringUtf8(128)));

    /** The spells bound into a sketchbook, and which is selected. */
    public static final Supplier<DataComponentType<BoundSpells>> BOUND_SPELLS =
            COMPONENTS.registerComponentType("bound_spells", builder -> builder
                    .persistent(BoundSpellsCodecs.CODEC)
                    .networkSynchronized(BoundSpellsCodecs.STREAM_CODEC));

    /** How much ink a vial is carrying, and of what grade. */
    public static final Supplier<DataComponentType<InkCharge>> INK_CHARGE =
            COMPONENTS.registerComponentType("ink_charge", builder -> builder
                    .persistent(InkCharge.CODEC)
                    .networkSynchronized(InkCharge.STREAM_CODEC));

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}