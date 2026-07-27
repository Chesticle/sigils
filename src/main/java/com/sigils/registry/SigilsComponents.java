package com.sigils.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;
import com.sigils.core.spell.CompiledSpell;

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

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}