package com.sigils.command;

import com.mojang.brigadier.context.CommandContext;
import com.sigils.registry.ElementDefinition;
import com.sigils.registry.EffectHandlerType;
import com.sigils.registry.ReactionRuleDefinition;
import com.sigils.registry.SigilsRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.ArrayList;
import java.util.List;
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

        // Load every reaction rule from the datapack registry into core types.
        Registry<ReactionRuleDefinition> registry =
                ctx.getSource().registryAccess().lookupOrThrow(SigilsRegistries.REACTION);
        List<ReactionRule> rules = new ArrayList<>();
        for (Identifier id : registry.keySet()) {
            ReactionRuleDefinition def = registry.getValue(id);
            if (def != null) {
                rules.add(def.toCore(id));
            }
        }

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
}