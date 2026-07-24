package com.sigils.command;

import com.mojang.brigadier.context.CommandContext;
import com.sigils.registry.ElementDefinition;
import com.sigils.registry.EffectHandlerType;
import com.sigils.registry.SigilsRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
}