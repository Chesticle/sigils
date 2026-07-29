package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import com.sigils.core.spell.CompiledSpell;
import com.sigils.registry.SigilsComponents;


/** Turns an inscribed sheet into a mark on a surface. */
public final class WorldSigilPlacement {

    private WorldSigilPlacement() {}

    /**
     * Draw {@code sheet}'s spell onto the face that was clicked.
     *
     * @return CONSUME on success, PASS if this sheet can't be placed here — PASS
     *         and not FAIL, so the item's own {@code use} still gets its turn and
     *         clicking a wall you can't draw on falls through to casting rather
     *         than doing nothing at all.
     */
    /** The state a sigil takes when freshly drawn: facing outward, lit, unworn. */
    public static BlockState stateFor(Direction face) {
        return SigilsBlocks.WORLD_SIGIL.get().defaultBlockState()
                .setValue(WorldSigilBlock.FACING, face)
                .setValue(WorldSigilBlock.LIT, true)
                .setValue(WorldSigilBlock.WEAR, 0);
    }

    /** Whether a sigil could exist at {@code target} facing {@code face}. */
    public static boolean canStamp(Level level, BlockPos target, Direction face) {
        return level.getBlockState(target).canBeReplaced()
                && stateFor(face).canSurvive(level, target);
    }

    /**
     * Draw a spell onto a surface. The world-facing half, with no item and no
     * player — which is what lets a machine call it in Part D.
     *
     * @return false if nothing was drawn
     */
    public static boolean stamp(Level level, BlockPos target, Direction face,
                                CompiledSpell spell, @Nullable String inkGradeId) {
        if (level.isClientSide() || spell == null || !canStamp(level, target, face)) {
            return false;
        }
        BlockState sigil = stateFor(face);

        // UPDATE_NEIGHBORS only — deliberately NOT UPDATE_CLIENTS. The block
        // entity is filled in below and syncs itself, so the first thing the
        // client hears about this position already carries the spell and the ink.
        level.setBlock(target, sigil, Block.UPDATE_NEIGHBORS);

        if (level.getBlockEntity(target) instanceof WorldSigilBlockEntity entity) {
            entity.inscribe(spell, inkGradeId);
        } else {
            // Shouldn't happen, but never leave a block the client can't see.
            level.sendBlockUpdated(target, sigil, sigil, Block.UPDATE_ALL);
        }
        // Puts the placement flare out, and guarantees a section rebuild at a
        // point where the block entity has certainly been synced.
        level.scheduleTick(target, sigil.getBlock(), WorldSigilBlock.FLASH_TICKS);
        return true;
    }

    /**
     * Draw {@code sheet}'s spell onto the face that was clicked.
     *
     * @return CONSUME on success, PASS if this sheet can't be placed here — PASS
     *         and not FAIL, so the item's own {@code use} still gets its turn and
     *         clicking a wall you can't draw on falls through to casting.
     */
    public static InteractionResult place(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack sheet = context.getItemInHand();
        CompiledSpell spell = sheet.get(SigilsComponents.SPELL.get());
        if (spell == null) {
            return InteractionResult.PASS; // a blank sheet draws nothing
        }

        Direction face = context.getClickedFace();
        BlockPos target = context.getClickedPos().relative(face);

        if (level.isClientSide()) {
            return canStamp(level, target, face)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        if (!stamp(level, target, face, spell, sheet.get(SigilsComponents.INK_GRADE.get()))) {
            return InteractionResult.PASS;
        }

        level.playSound(null, target, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 0.8f, 0.9f);

        Player player = context.getPlayer();
        if (player == null || !player.hasInfiniteMaterials()) {
            sheet.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}