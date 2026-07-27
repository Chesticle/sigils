package com.sigils.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sigils.cast.CastContext;
import com.sigils.cast.SpellCaster;
import com.sigils.cast.SpellCasting;
import com.sigils.core.draft.DraftLimits;
import com.sigils.core.draft.DraftValidator;
import com.sigils.core.draft.InkCost;
import com.sigils.core.draft.PenCapabilities;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphLookup;
import com.sigils.core.particle.ParticleProfile;
import com.sigils.core.particle.ProfileLookup;
import com.sigils.core.particle.SpellVisuals;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.core.spell.ValidationResult;
import com.sigils.registry.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.List;
import java.util.Optional;

import com.sigils.core.element.ElementalMixture;
import com.sigils.core.reaction.PhenomenonResolver;
import com.sigils.core.reaction.ReactionRule;
import com.sigils.core.reaction.Resolution;
import com.sigils.registry.InkGradeDefinition;
import com.sigils.registry.PenTierDefinition;
import com.sigils.registry.SigilsInks;
import com.sigils.registry.SigilsPens;

/**
 * Debug commands.
 *
 * <p>{@code /sigils elements} is the single most important thing in Phase 0:
 * it proves that content defined purely in JSON reaches the game. Keep it
 * around — you will use it every time you add a datapack registry.
 */
public final class SigilsCommands {

    private SigilsCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sigils")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                        .then(Commands.literal("elements")
                                .executes(SigilsCommands::listElements))
                        .then(Commands.literal("handlers")
                                .executes(SigilsCommands::listHandlers))
                        .then(Commands.literal("simulate")
                                .then(Commands.argument("fire", FloatArgumentType.floatArg(0f))
                                        .then(Commands.argument("water", FloatArgumentType.floatArg(0f))
                                                .then(Commands.argument("earth", FloatArgumentType.floatArg(0f))
                                                        .then(Commands.argument("air", FloatArgumentType.floatArg(0f))
                                                                .executes(SigilsCommands::simulate))))))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                c.getSource().registryAccess().lookupOrThrow(SigilsRegistries.SPELL)
                                                        .keySet().stream().map(Identifier::toString), b))
                                        .executes(SigilsCommands::cast)))
                        .then(Commands.literal("visualize")
                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                c.getSource().registryAccess().lookupOrThrow(SigilsRegistries.SPELL)
                                                        .keySet().stream().map(Identifier::toString), b))
                                        .executes(SigilsCommands::visualize)))
                        .then(Commands.literal("glyphs")
                                .executes(SigilsCommands::listGlyphs))
                        .then(Commands.literal("draft")
                                .executes(SigilsCommands::checkDemoDraft))
                        .then(Commands.literal("pens")
                                .executes(SigilsCommands::listPens))
                        .then(Commands.literal("inks")
                                .executes(SigilsCommands::listInks))
        );
    }

    /** Lists everything loaded from the datapack element registry. */
    private static int listElements(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<ElementDefinition> registry =
                source.registryAccess().lookupOrThrow(SigilsRegistries.ELEMENT);

        source.sendSuccess(() -> Component.literal("Loaded elements:"), false);

        int count = 0;
        for (Identifier id : registry.keySet()) {
            ElementDefinition def = registry.getValue(id);
            if (def == null) continue;
            count++;
            String line = String.format(
                    "  %s  colour=#%06X  density=%.2f  volatility=%.2f  luminance=%.2f",
                    id, def.colorLinear(), def.density(), def.volatility(), def.luminance());
            source.sendSuccess(() -> Component.literal(line), false);
        }

        final int total = count;
        source.sendSuccess(() -> Component.literal(total + " element(s) from datapacks"), false);
        return total;
    }

    /** Lists everything in the code-backed effect handler registry. */
    private static int listHandlers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<EffectHandlerType> registry = SigilsRegistries.EFFECT_HANDLER_REGISTRY;

        source.sendSuccess(() -> Component.literal("Registered effect handlers:"), false);

        int count = 0;
        for (Identifier id : registry.keySet()) {
            EffectHandlerType handler = registry.getValue(id);
            if (handler == null) continue;
            count++;
            String line = "  " + id + " — " + handler.describe();
            source.sendSuccess(() -> Component.literal(line), false);
        }

        final int total = count;
        source.sendSuccess(() -> Component.literal(total + " handler(s) from code"), false);
        return total;
    }

    private static int simulate(CommandContext<CommandSourceStack> ctx) {
        float fire = FloatArgumentType.getFloat(ctx, "fire");
        float water = FloatArgumentType.getFloat(ctx, "water");
        float earth = FloatArgumentType.getFloat(ctx, "earth");
        float air = FloatArgumentType.getFloat(ctx, "air");

        ElementalMixture mixture = ElementalMixture.EMPTY
                .plus(ElementalMixture.of("sigils:fire", fire))
                .plus(ElementalMixture.of("sigils:water", water))
                .plus(ElementalMixture.of("sigils:earth", earth))
                .plus(ElementalMixture.of("sigils:air", air));

        //Loads data pack rules from SigilsReactions script.
        List<ReactionRule> rules = SigilsReactions.load(ctx.getSource().registryAccess());

        Resolution result = new PhenomenonResolver().resolve(mixture, rules);

        ctx.getSource().sendSuccess(() -> Component.literal("Input: " + mixture), false);
        if (result.isInert()) {
            ctx.getSource().sendSuccess(() -> Component.literal("  (no reaction)"), false);
        } else {
            result.phenomena().forEach((phenomenon, strength) ->
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "  phenomenon " + phenomenon + " x" + String.format("%.2f", strength)), false));
        }
        ctx.getSource().sendSuccess(() -> Component.literal("  residual " + result.residual()), false);
        return 1;
    }

    private static int cast(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        String arg = StringArgumentType.getString(ctx, "spell");
        Identifier spellId = Identifier.tryParse(arg);
        if (spellId == null) {
            source.sendFailure(Component.literal("Not a valid spell id: " + arg));
            return 0;
        }

        Registry<SpellDefinition> spells = source.registryAccess().lookupOrThrow(SigilsRegistries.SPELL);
        SpellDefinition def = spells.getValue(spellId);
        if (def == null) {
            source.sendFailure(Component.literal("No spell registered as " + spellId));
            return 0;
        }

        ServerLevel level = source.getLevel();
        ServerPlayer caster = source.getEntity() instanceof ServerPlayer player ? player : null;
        Vec3 origin = caster != null ? caster.getEyePosition() : source.getPosition();

        // Begin a budget-guarded cast. Empty means this tick is already full.
        Optional<CastContext> maybeCtx = SpellCasting.begin(level, caster, origin);
        if (maybeCtx.isEmpty()) {
            source.sendFailure(Component.literal("Spell budget for this tick is exhausted."));
            return 0;
        }

        CompiledSpell spell = def.toCompiled();
        List<ReactionRule> rules = SigilsReactions.load(source.registryAccess());
        SpellCaster.cast(maybeCtx.get(), spell, rules);

        source.sendSuccess(() -> Component.literal("Cast " + spellId), false);
        return 1;
    }

    private static int visualize(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        String arg = StringArgumentType.getString(ctx, "spell");
        Identifier spellId = Identifier.tryParse(arg);
        if (spellId == null) {
            source.sendFailure(Component.literal("Not a valid spell id: " + arg));
            return 0;
        }

        Registry<SpellDefinition> spells = source.registryAccess().lookupOrThrow(SigilsRegistries.SPELL);
        SpellDefinition def = spells.getValue(spellId);
        if (def == null) {
            source.sendFailure(Component.literal("No spell registered as " + spellId));
            return 0;
        }

        CompiledSpell spell = def.toCompiled();
        List<ReactionRule> rules = SigilsReactions.load(source.registryAccess());
        Resolution resolution = new PhenomenonResolver().resolve(spell.mixture(), rules);

        ProfileLookup lookup = SigilsProfiles.lookup(source.registryAccess());
        Optional<ParticleProfile> blended = SpellVisuals.blend(resolution, lookup);
        if (blended.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Visualize " + spellId + ": inert — no visual"), false);
            return 1;
        }

        float instability = spell.baseInstability();
        ParticleProfile profile = blended.get().perturbed(instability);

        source.sendSuccess(() -> Component.literal("Visualize " + spellId
                + "  (fidelity " + fmt(spell.fidelity())
                + ", instability " + fmt(instability) + ")"), false);
        source.sendSuccess(() -> Component.literal("  colour     " + toHex(profile)
                + "   linear(" + fmt(profile.red()) + ", " + fmt(profile.green()) + ", " + fmt(profile.blue()) + ")"), false);
        source.sendSuccess(() -> Component.literal("  motion     size " + fmt(profile.size())
                + "  speed " + fmt(profile.speed()) + "  gravity " + fmt(profile.gravity())), false);
        source.sendSuccess(() -> Component.literal("  character  turbulence " + fmt(profile.turbulence())
                + "  emissive " + fmt(profile.emissive()) + "  density " + fmt(profile.density())), false);
        return 1;
    }
    /** Lists everything loaded from the datapack glyph registry. */
    private static int listGlyphs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<GlyphDefinition> registry =
                source.registryAccess().lookupOrThrow(SigilsRegistries.GLYPH);

        source.sendSuccess(() -> Component.literal("Loaded glyphs:"), false);

        int count = 0;
        for (Identifier id : registry.keySet()) {
            GlyphDefinition definition = registry.getValue(id);
            if (definition == null) continue;
            count++;
            Glyph glyph = definition.toCore(id);
            String detail = switch (glyph.role()) {
                case CREST -> "  " + glyph.contribution().map(Object::toString).orElse("no mixture");
                case MODIFIER -> "  " + glyph.operation().map(Object::toString).orElse("no operation");
                default -> "";
            };
            String line = String.format(
                    "  %s  role=%s strokes=%d tol=%.2f complexity=%d ink=%.1f%s",
                    id, glyph.role(), glyph.strokes().size(), glyph.toleranceBand(),
                    glyph.complexity(), glyph.inkCost(), detail);
            source.sendSuccess(() -> Component.literal(line), false);
        }

        final int total = count;
        source.sendSuccess(() -> Component.literal(total + " glyph(s) from datapacks"), false);
        return total;
    }

    /**
     * Runs a hardcoded three-glyph draft through the real registry-backed lookup and
     * the real validator — exactly the two calls the canvas screen will make in
     * Part C, minus the pixels.
     */
    private static int checkDemoDraft(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GlyphLookup glyphs = SigilsGlyphs.lookup(source.registryAccess());

        List<GlyphInstance> draft = List.of(
                new GlyphInstance("sigils:ring_basic", new Vec2(0.5f, 0.5f), 0f, 1.0f),
                new GlyphInstance("sigils:crest_fire", new Vec2(0.5f, 0.5f), 0f, 0.35f),
                new GlyphInstance("sigils:mod_beam", new Vec2(0.5f, 0.72f), 0f, 0.25f));

        ValidationResult result = DraftValidator.validate(draft, glyphs, DraftLimits.DRAFTING_TABLE);
        float ink = InkCost.of(draft, glyphs);

        source.sendSuccess(() -> Component.literal(
                String.format("Demo draft: %d glyph(s), ink cost %.2f", draft.size(), ink)), false);

        if (result.valid()) {
            source.sendSuccess(() -> Component.literal("  ✔ castable — this draft would compile"), false);
            return 1;
        }
        for (String error : result.errors()) {
            source.sendSuccess(() -> Component.literal("  ✘ " + error), false);
        }
        return 0;
    }

    /** Lists every pen tier and the item it binds. */
    private static int listPens(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<PenTierDefinition> registry =
                source.registryAccess().lookupOrThrow(SigilsRegistries.PEN_TIER);

        source.sendSuccess(() -> Component.literal("Pen tiers:"), false);

        for (Identifier id : registry.keySet()) {
            PenTierDefinition definition = registry.getValue(id);
            if (definition == null) continue;
            PenCapabilities pen = definition.toCore();
            DraftLimits limits = pen.limits();
            String line = String.format(
                    "  %s  item=%s glyphs=%d crests=%d complexity=%d radius=%.2f rings=%s"
                            + " wobble=x%.2f floor=%.2f",
                    id, definition.item(), limits.maxGlyphs(), limits.maxCrests(),
                    limits.maxComplexity(), limits.canvasRadius(),
                    limits.allowMultipleRings() ? "many" : "one",
                    pen.instabilityFactor(), pen.instabilityFloor());
            source.sendSuccess(() -> Component.literal(line), false);
        }

        // What the table actually resolved to — a tier whose item is missing won't be here.
        int bound = SigilsPens.table(source.registryAccess()).size();
        source.sendSuccess(() -> Component.literal(bound + " item(s) bound as pens"), false);
        return bound;
    }

    /** Lists every ink grade and the item it binds. */
    private static int listInks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<InkGradeDefinition> registry =
                source.registryAccess().lookupOrThrow(SigilsRegistries.INK_GRADE);

        source.sendSuccess(() -> Component.literal("Ink grades:"), false);

        for (Identifier id : registry.keySet()) {
            InkGradeDefinition definition = registry.getValue(id);
            if (definition == null) continue;
            String line = String.format("  %s  item=%s units=%.1f permanent=%s tint=#%06X",
                    id, definition.item(), definition.unitsPerItem(),
                    definition.permanent(), definition.tint());
            source.sendSuccess(() -> Component.literal(line), false);
        }

        int bound = SigilsInks.table(source.registryAccess()).size();
        source.sendSuccess(() -> Component.literal(bound + " item(s) bound as ink"), false);
        return bound;
    }

    private static String fmt(float v) {
        return String.format("%.3f", v);
    }

    /** Linear profile colour → sRGB hex, for a readable printout only. */
    private static String toHex(ParticleProfile p) {
        return String.format("#%02X%02X%02X", to8(p.red()), to8(p.green()), to8(p.blue()));
    }

    private static int to8(float linear) {
        float s = linear <= 0.0031308f
                ? linear * 12.92f
                : 1.055f * (float) Math.pow(linear, 1f / 2.4f) - 0.055f;
        return Math.clamp(Math.round(s * 255f), 0, 255);
    }
}