package com.sigils.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

import com.sigils.knowledge.KnowledgeSources;
import com.sigils.knowledge.SigilsKnowledge;
import com.sigils.registry.SigilsComponents;
import com.sigils.registry.SigilsGlyphs;

/**
 * A stone tablet carrying one glyph. Right-click to study it.
 *
 * <p>There is one tablet item and one component. Nine glyphs, nine tablets, one
 * class — and a tenth glyph from somebody else's datapack gets a tablet without
 * this file being opened.
 *
 * <p>Note what this class does <em>not</em> do on success: no message, no sound,
 * no particles. All of that belongs to {@code KnowledgeSources.TABLET}, so that
 * a trade in Part E can feel like a purchase and a command can feel like
 * nothing, without either of them reimplementing what a grant is.
 */
public class GlyphTabletItem extends Item {

    public GlyphTabletItem(Properties properties) {
        super(properties);
    }

    /** The glyph this tablet teaches, if it was given one. */
    public static Optional<String> glyphOf(ItemStack stack) {
        return Optional.ofNullable(stack.get(SigilsComponents.GLYPH_REF.get()));
    }

    /** A tablet for one glyph. Used by the creative tab and, later, by loot. */
    public static ItemStack of(String glyphId) {
        ItemStack tablet = new ItemStack(SigilsItems.GLYPH_TABLET.get());
        tablet.set(SigilsComponents.GLYPH_REF.get(), glyphId);
        return tablet;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack tablet = player.getItemInHand(hand);

        // The client swings the arm and stops. Everything below is authority.
        if (level.isClientSide() || !(player instanceof ServerPlayer student)) {
            return InteractionResult.SUCCESS;
        }

        String glyphId = glyphOf(tablet).orElse(null);
        if (glyphId == null || !SigilsGlyphs.exists(level.registryAccess(), glyphId)) {
            // Either a tablet nobody set a glyph on, or one whose datapack has
            // since been removed. Say so, and do not eat it — reinstall the pack
            // and the tablet works again.
            student.sendSystemMessage(Component.translatable("message.sigils.tablet.unreadable"));
            return InteractionResult.FAIL;
        }

        if (!SigilsKnowledge.grant(student, glyphId, KnowledgeSources.TABLET)) {
            student.sendSystemMessage(Component.translatable(
                    "message.sigils.tablet.known",
                    Component.translatable(SigilsGlyphs.nameKey(glyphId))
                            .withStyle(ChatFormatting.GOLD)));
            return InteractionResult.FAIL;
        }

        tablet.consume(1, student);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Optional<String> glyphId = glyphOf(stack);
        if (glyphId.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.sigils.tablet.blank")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.accept(Component.translatable(
                        "tooltip.sigils.tablet.teaches",
                        Component.translatable(SigilsGlyphs.nameKey(glyphId.get()))
                                .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));
    }
}