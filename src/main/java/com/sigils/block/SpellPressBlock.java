package com.sigils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import com.sigils.core.sigil.PressReadiness;

/**
 * A machine that draws. Load a master parchment, a pen and ink; pulse it with
 * redstone and it stamps the spell onto the cell in front of its face.
 *
 * <p>Piston-like in the literal sense: it reaches exactly one block, and if you
 * want it to reach further you move the press.
 */
public class SpellPressBlock extends BaseEntityBlock {

    public static final MapCodec<SpellPressBlock> CODEC = simpleCodec(SpellPressBlock::new);

    /** The direction the press prints in. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public SpellPressBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpellPressBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Faces the player, like a dispenser — so placing one while looking down
        // at the floor gives you a press that prints upward, which is the trap
        // everyone builds first.
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);

        if (level.getBlockEntity(pos) instanceof SpellPressBlockEntity press
                && level instanceof ServerLevel server) {
            press.onNeighborChanged(server);
        }
    }

    // --------------------------------------------------------------- comparators

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SpellPressBlockEntity press
                ? press.comparatorOutput()
                : 0;
    }

    // --------------------------------------------------------------- interaction

    /** Right-click with a pen, ink or an inscribed sheet to load that slot. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.isEmpty()) {
            // NOT PASS — this is the sentinel that lets useWithoutItem run.
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof SpellPressBlockEntity press)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return press.load(stack, player, hand)
                ? InteractionResult.CONSUME
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Empty hand reports status; sneaking empties it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof SpellPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            Containers.dropContents(level, pos.relative(state.getValue(FACING)), press);
            press.clearContent();
            return InteractionResult.CONSUME;
        }

        PressReadiness readiness = press.readiness();
        player.sendSystemMessage(Component.translatable(
                "message.sigils.press." + readiness.name().toLowerCase(java.util.Locale.ROOT),
                press.inkSummary()));
        return InteractionResult.CONSUME;
    }
}