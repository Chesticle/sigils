package com.sigils.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sigils.cast.CastContext;
import com.sigils.cast.SpellCaster;
import com.sigils.cast.SpellCasting;
import com.sigils.core.spell.CompiledSpell;
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
}