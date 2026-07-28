package com.sigils.registry;

import com.sigils.Sigils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * Every registry the mod owns, in one place.
 *
 * <p>There are two kinds, and choosing correctly matters:
 *
 * <ul>
 *   <li><b>Datapack registries</b> hold <em>content</em> defined in JSON and
 *       loaded at world load: elements, phenomena, reaction rules, glyphs.
 *       Datapack makers and addon authors can add these with no code.</li>
 *   <li><b>Built-in registries</b> hold <em>behaviour</em> defined in Java and
 *       loaded at game start: effect handlers, delivery shapes, target
 *       selectors. Addon mods can add these, but datapacks cannot.</li>
 * </ul>
 *
 * <p>When in doubt, ask: "could someone express this in JSON?" If yes, it's a
 * datapack registry.
 */
public final class SigilsRegistries {

    private SigilsRegistries() {}

    // ================================================================
    // Datapack registries — content, from JSON, at world load
    // ================================================================

    /**
     * Elements.
     *
     * <p>JSON for this registry lives at:
     * <pre>data/&lt;pack_namespace&gt;/sigils/element/&lt;name&gt;.json</pre>
     *
     * <p>For our own built-in content that is
     * {@code data/sigils/sigils/element/fire.json}. The doubled "sigils" is
     * <em>correct and required</em>: the first is the datapack namespace, the
     * second is our registry's namespace. Put the file one level up and it will
     * be silently ignored with no error message.
     */
    public static final ResourceKey<Registry<ElementDefinition>> ELEMENT =
            ResourceKey.createRegistryKey(Sigils.id("element"));

    public static final ResourceKey<Registry<PhenomenonDefinition>> PHENOMENON =
            ResourceKey.createRegistryKey(Sigils.id("phenomenon"));

    public static final ResourceKey<Registry<ReactionRuleDefinition>> REACTION =
            ResourceKey.createRegistryKey(Sigils.id("reaction"));

    public static final ResourceKey<Registry<SpellDefinition>> SPELL =
            ResourceKey.createRegistryKey(Sigils.id("spell"));

    public static final ResourceKey<Registry<ParticleProfileDefinition>> PARTICLE_PROFILE =
            ResourceKey.createRegistryKey(Sigils.id("particle_profile"));

    public static final ResourceKey<Registry<GlyphDefinition>> GLYPH =
            ResourceKey.createRegistryKey(Sigils.id("glyph"));

    /** Pen tiers. {@code data/<pack>/sigils/pen_tier/<name>.json} */
    public static final ResourceKey<Registry<PenTierDefinition>> PEN_TIER =
            ResourceKey.createRegistryKey(Sigils.id("pen_tier"));

    /** Ink grades. {@code data/<pack>/sigils/ink_grade/<name>.json} */
    public static final ResourceKey<Registry<InkGradeDefinition>> INK_GRADE =
            ResourceKey.createRegistryKey(Sigils.id("ink_grade"));

    /** Parchment grades. {@code data/<pack>/sigils/parchment_grade/<name>.json} */
    public static final ResourceKey<Registry<ParchmentGradeDefinition>> PARCHMENT_GRADE =
            ResourceKey.createRegistryKey(Sigils.id("parchment_grade"));

    // ================================================================
    // Built-in registries — behaviour, from Java, at game start
    // ================================================================

    public static final ResourceKey<Registry<EffectHandlerType>> EFFECT_HANDLER_KEY =
            ResourceKey.createRegistryKey(Sigils.id("effect_handler"));

    public static final Registry<EffectHandlerType> EFFECT_HANDLER_REGISTRY =
            new RegistryBuilder<>(EFFECT_HANDLER_KEY)
                    .sync(true) // clients need these for prediction and rendering later
                    .create();

    private static final DeferredRegister<EffectHandlerType> EFFECT_HANDLERS =
            DeferredRegister.create(EFFECT_HANDLER_REGISTRY, Sigils.MOD_ID);

    // Two placeholder handlers so there is something to list.
    // Phase 2 replaces these with real world-mutating implementations.
    public static final Supplier<EffectHandlerType> IGNITE =
            EFFECT_HANDLERS.register("ignite",
                    () -> EffectHandlerType.simple("Sets fire to entities and flammable blocks"));

    public static final Supplier<EffectHandlerType> DOUSE =
            EFFECT_HANDLERS.register("douse",
                    () -> EffectHandlerType.simple("Extinguishes fire and wets surfaces"));

    // ================================================================
    // Wiring
    // ================================================================

    /** Called once, from the mod constructor. */
    public static void register(IEventBus modBus) {
        modBus.addListener(SigilsRegistries::onNewRegistry);
        modBus.addListener(SigilsRegistries::onNewDataPackRegistry);
        EFFECT_HANDLERS.register(modBus);
    }

    private static void onNewRegistry(NewRegistryEvent event) {
        event.register(EFFECT_HANDLER_REGISTRY);
    }


    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
                ELEMENT,
                ElementDefinition.CODEC,  // disk/server codec
                ElementDefinition.CODEC   // network codec — clients need colours for particles

        );
        event.dataPackRegistry(PHENOMENON, PhenomenonDefinition.CODEC, PhenomenonDefinition.CODEC);
        event.dataPackRegistry(REACTION, ReactionRuleDefinition.CODEC, ReactionRuleDefinition.CODEC);
        event.dataPackRegistry(SPELL, SpellDefinition.CODEC, SpellDefinition.CODEC);
        event.dataPackRegistry(PARTICLE_PROFILE, ParticleProfileDefinition.CODEC, ParticleProfileDefinition.CODEC);
        event.dataPackRegistry(
                GLYPH,
                GlyphDefinition.CODEC,  // disk/server codec
                GlyphDefinition.CODEC   // network codec — the client draws the palette from this
        );
        event.dataPackRegistry(
                PEN_TIER,
                PenTierDefinition.CODEC,   // disk/server
                PenTierDefinition.CODEC);  // network — the palette filter runs client-side

        event.dataPackRegistry(
                INK_GRADE,
                InkGradeDefinition.CODEC,
                InkGradeDefinition.CODEC); // network — the ink bar needs capacity and tint

        event.dataPackRegistry(
                PARCHMENT_GRADE,
                ParchmentGradeDefinition.CODEC,
                ParchmentGradeDefinition.CODEC); // network — the slot filter runs client-side too
    }
}