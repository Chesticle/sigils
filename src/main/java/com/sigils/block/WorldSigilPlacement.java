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
    public static InteractionResult place(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack sheet = context.getItemInHand();
        CompiledSpell spell = sheet.get(SigilsComponents.SPELL.get());
        if (spell == null) {
            return InteractionResult.PASS; // a blank sheet draws nothing
        }

        Direction face = context.getClickedFace();
        BlockPos target = context.getClickedPos().relative(face);

        // Placed LIT: the ink flares as it's drawn, and — the reason this is here
        // rather than in a polish pass — the scheduled tick that puts it out ten
        // ticks later is a guaranteed block state change, which forces a section
        // rebuild at a moment when the block entity has certainly been synced.
        BlockState sigil = SigilsBlocks.WORLD_SIGIL.get().defaultBlockState()
                .setValue(WorldSigilBlock.FACING, face)
                .setValue(WorldSigilBlock.LIT, true);

        if (!level.getBlockState(target).canBeReplaced() || !sigil.canSurvive(level, target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // UPDATE_NEIGHBORS only — deliberately NOT UPDATE_CLIENTS. The block
        // entity is filled in below and syncs itself, so the first thing the
        // client hears about this position already carries the spell and the ink.
        // Placing with UPDATE_ALL races: the chunk mesh can be rebuilt against an
        // empty block entity, and a baked tint doesn't re-evaluate on its own.
        level.setBlock(target, sigil, Block.UPDATE_NEIGHBORS);

        if (level.getBlockEntity(target) instanceof WorldSigilBlockEntity entity) {
            entity.inscribe(spell, sheet.get(SigilsComponents.INK_GRADE.get()));
        } else {
            // Shouldn't happen, but never leave a block the client can't see.
            level.sendBlockUpdated(target, sigil, sigil, Block.UPDATE_ALL);
        }

        level.scheduleTick(target, sigil.getBlock(), WorldSigilBlock.FLASH_TICKS);

        level.playSound(null, target, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 0.8f, 0.9f);

        Player player = context.getPlayer();
        if (player == null || !player.hasInfiniteMaterials()) {
            sheet.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}