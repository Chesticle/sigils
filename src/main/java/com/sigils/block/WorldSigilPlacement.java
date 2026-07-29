package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

import net.minecraft.server.level.ServerLevel;

import com.sigils.core.sigil.PlaneOffset;
import com.sigils.core.sigil.SigilFootprint;


/** Turns an inscribed sheet into a mark on a surface. */
public final class WorldSigilPlacement {

    private WorldSigilPlacement() {}

    /**
     * Draw a member cell — a piece of a ring, carrying no spell of its own.
     */
    public static boolean stampMember(Level level, BlockPos target, Direction face,
                                      BlockPos core, @Nullable String inkGradeId) {
        if (level.isClientSide() || !canStamp(level, target, face)) {
            return false;
        }
        BlockState sigil = stateFor(face);
        level.setBlock(target, sigil, Block.UPDATE_NEIGHBORS);
        if (level.getBlockEntity(target) instanceof WorldSigilBlockEntity entity) {
            entity.joinStructure(core, inkGradeId);
        }
        level.scheduleTick(target, sigil.getBlock(), WorldSigilBlock.FLASH_TICKS);
        return true;
    }

    /**
     * Lay out the largest ring that fits, down to a single cell.
     *
     * <p>Shrink-to-fit rather than a chosen radius: choosing needs a UI, and this
     * turns out to be the better mechanic anyway — a circle drawn in a corridor is
     * small and the same sheet drawn in a field is not.
     *
     * @return the radius actually drawn, or -1 if not even one cell would go
     */
    public static int stampStructure(ServerLevel level, BlockPos core, Direction face,
                                     int maxRadius, CompiledSpell spell,
                                     @Nullable String inkGradeId) {

        int cap = Math.clamp(maxRadius, 0, SigilFootprint.MAX_RADIUS);
        for (int radius = cap; radius >= 1; radius--) {
            if (!fits(level, core, face, radius)) {
                continue;
            }
            if (!stamp(level, core, face, spell, inkGradeId)) {
                return -1;
            }
            for (PlaneOffset offset : SigilFootprint.ring(radius)) {
                stampMember(level, WorldSigilBlockEntity.inPlane(core, face, offset),
                        face, core, inkGradeId);
            }
            if (level.getBlockEntity(core) instanceof WorldSigilBlockEntity entity) {
                entity.becomeCore(radius);
            }
            return radius;
        }
        return stamp(level, core, face, spell, inkGradeId) ? 0 : -1;
    }

    /** Every cell clear, and no existing sigil anywhere inside the footprint. */
    private static boolean fits(ServerLevel level, BlockPos core, Direction face, int radius) {
        if (!SigilIndex.of(level).within(core, radius + 1).isEmpty()) {
            return false; // don't draw a circle through someone else's
        }
        for (PlaneOffset offset : SigilFootprint.all(radius)) {
            if (!canStamp(level, WorldSigilBlockEntity.inPlane(core, face, offset), face)) {
                return false;
            }
        }
        return true;
    }

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
        Integer reach = sheet.get(SigilsComponents.SIGIL_RADIUS.get());
        String ink = sheet.get(SigilsComponents.INK_GRADE.get());

        int drawn = level instanceof ServerLevel server && reach != null && reach > 0
                ? stampStructure(server, target, face, reach, spell, ink)
                : (stamp(level, target, face, spell, ink) ? 0 : -1);

        if (drawn < 0) {
            return InteractionResult.PASS;
        }
        Player drawer = context.getPlayer();
        if (drawn > 0 && drawer != null) {
            drawer.sendSystemMessage(Component.translatable(
                    "message.sigils.sigil.circle_drawn", drawn));
        }

        level.playSound(null, target, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 0.8f, 0.9f);

        if (drawer == null || !drawer.hasInfiniteMaterials()) {
            sheet.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}