package com.sigils.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.sigils.cast.CastContext;
import com.sigils.cast.SpellCaster;
import com.sigils.cast.SpellCasting;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsReactions;

/**
 * A sheet of parchment. Blank until inscribed; once it carries a
 * {@code sigils:spell} component, right-clicking casts it and consumes it.
 *
 * <p>Casting from here reuses the Phase 2 pipeline verbatim — guard, resolve,
 * deliver, dispatch — and therefore the Phase 3 emitter too. This item is the
 * "debug wand" the roadmap wanted in Phase 2, arriving late but permanent.
 */
public class ParchmentItem extends Item {

    public ParchmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        CompiledSpell spell = held.get(SigilsComponents.SPELL.get());
        if (spell == null) {
            return InteractionResult.PASS; // blank parchment does nothing
        }
        if (level.isClientSide() || !(player instanceof ServerPlayer caster)) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Optional<CastContext> context =
                SpellCasting.begin(serverLevel, caster, caster.getEyePosition());
        if (context.isEmpty()) {
            return InteractionResult.FAIL; // this tick's spell budget is full
        }

        SpellCaster.cast(context.get(), spell, SigilsReactions.load(serverLevel.registryAccess()));

        if (!player.hasInfiniteMaterials()) {
            held.shrink(1); // one casting per sheet — artifacts that persist arrive in Phase 6
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        CompiledSpell spell = stack.get(SigilsComponents.SPELL.get());
        if (spell == null) {
            tooltip.accept(Component.translatable("tooltip.sigils.parchment.blank")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (Map.Entry<String, Float> entry : spell.mixture().asMap().entrySet()) {
            tooltip.accept(Component.literal(
                            String.format("  %s x%.2f", entry.getKey(), entry.getValue()))
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.accept(Component.literal(String.format("  %s, fidelity %.2f",
                        spell.delivery().shapeId(), spell.fidelity()))
                .withStyle(ChatFormatting.GRAY));

        String grade = stack.get(SigilsComponents.INK_GRADE.get());
        if (grade != null) {
            // "sigils:netherite" -> "ink_grade.sigils.netherite", the same key the
            // ink vial uses, so one lang entry serves both.
            tooltip.accept(Component.translatable("tooltip.sigils.parchment.ink",
                            Component.translatable("ink_grade." + grade.replace(':', '.')))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}