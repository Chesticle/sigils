package com.sigils.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

import com.sigils.cast.CastContext;
import com.sigils.cast.SpellCasting;
import com.sigils.core.spell.BoundSpells;
import com.sigils.core.spell.CompiledSpell;
import com.sigils.draft.ParchmentGrades;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsInks;
import com.sigils.registry.SigilsReactions;
import com.sigils.cast.SpellCaster;

/**
 * A book of finished spells.
 *
 * <p>Right-click casts the selected one and keeps it. Sneak-right-click binds a
 * permanent-ink parchment out of your pack. Sneak-scroll changes the selection —
 * that one lives in {@code SketchbookInput}, because it's an input event rather
 * than an item use.
 */
public class SketchbookItem extends Item {

    /** Ticks between casts. A bound spell is free; it isn't instant. */
    public static final int CAST_COOLDOWN_TICKS = 30;

    public SketchbookItem(Properties properties) {
        super(properties);
    }

    public static BoundSpells contentsOf(ItemStack stack) {
        BoundSpells bound = stack.get(SigilsComponents.BOUND_SPELLS.get());
        return bound == null ? BoundSpells.EMPTY : bound;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);

        if (level.isClientSide() || !(player instanceof ServerPlayer caster)) {
            return InteractionResult.SUCCESS;
        }
        return player.isShiftKeyDown()
                ? bind(book, caster)
                : castActive(book, caster, (ServerLevel) level);
    }

    // ------------------------------------------------------------------ casting

    private InteractionResult castActive(ItemStack book, ServerPlayer caster, ServerLevel level) {
        Optional<CompiledSpell> spell = contentsOf(book).activeSpell();
        if (spell.isEmpty()) {
            caster.sendSystemMessage(Component.translatable("message.sigils.sketchbook.empty"));
            return InteractionResult.FAIL;
        }

        Optional<CastContext> context =
                SpellCasting.begin(level, caster, caster.getEyePosition());
        if (context.isEmpty()) {
            return InteractionResult.FAIL; // this tick's spell budget is already full
        }

        SpellCaster.cast(context.get(), spell.get(), SigilsReactions.load(level.registryAccess()));
        caster.getCooldowns().addCooldown(book, CAST_COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ binding

    private InteractionResult bind(ItemStack book, ServerPlayer caster) {
        BoundSpells bound = contentsOf(book);
        if (bound.full()) {
            caster.sendSystemMessage(Component.translatable("message.sigils.sketchbook.full",
                    BoundSpells.MAX_PAGES));
            return InteractionResult.FAIL;
        }

        RegistryAccess registries = caster.level().registryAccess();
        Inventory inventory = caster.getInventory();
        boolean sawUnbindable = false;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty() || !ParchmentGrades.isParchment(registries, candidate)) {
                continue;
            }
            CompiledSpell spell = candidate.get(SigilsComponents.SPELL.get());
            if (spell == null) {
                continue; // blank sheet
            }
            if (!permanent(registries, candidate)) {
                sawUnbindable = true;
                continue;
            }

            book.set(SigilsComponents.BOUND_SPELLS.get(), bound.bind(spell));
            candidate.shrink(1);
            caster.level().playSound(null, caster.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9f, 1.0f);
            return InteractionResult.CONSUME;
        }

        caster.sendSystemMessage(Component.translatable(sawUnbindable
                ? "message.sigils.sketchbook.needs_permanent"
                : "message.sigils.sketchbook.nothing_to_bind"));
        return InteractionResult.FAIL;
    }

    /** Whether the ink that drew this parchment is one a book will hold. */
    private static boolean permanent(RegistryAccess registries, ItemStack parchment) {
        return SigilsInks.isPermanent(registries, parchment.get(SigilsComponents.INK_GRADE.get()));
    }

    // ------------------------------------------------------------- presentation

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contentsOf(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * contentsOf(stack).size() / BoundSpells.MAX_PAGES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF9A7B4F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        BoundSpells bound = contentsOf(stack);
        if (bound.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.sigils.sketchbook.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.accept(Component.translatable("tooltip.sigils.sketchbook.pages",
                        bound.size(), BoundSpells.MAX_PAGES)
                .withStyle(ChatFormatting.GRAY));

        for (int i = 0; i < bound.size(); i++) {
            CompiledSpell spell = bound.spells().get(i);
            boolean selected = i == bound.active();
            tooltip.accept(Component.literal(String.format("  %s %s  %.2f",
                            selected ? "▸" : " ",
                            spell.delivery().shapeId(),
                            spell.fidelity()))
                    .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
    }
}