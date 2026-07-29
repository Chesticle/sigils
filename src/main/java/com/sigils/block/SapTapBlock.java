package com.sigils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

import com.sigils.item.SigilsItems;
import com.sigils.registry.SigilsBlockTags;

/**
 * A spile driven into a silverwood trunk. Fills slowly, then hands over sap.
 *
 * <p>State only — no block entity. A fill level is three discrete steps, and
 * three steps fit in a block state, which means no per-tick work, no NBT, and
 * no save/load path to get wrong. Phase 6C had to invent a discrete shadow for a
 * continuous quantity; this one is discrete from the start.
 *
 * <p>Filling runs on random ticks, so a tap only works in a chunk somebody is
 * near. That's the honey and resin behaviour and it's the correct one: sap you
 * have to come back for is a reason to come back.
 */
public class SapTapBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<SapTapBlock> CODEC = simpleCodec(SapTapBlock::new);

    /** 0 empty, 1 running, 2 ready to draw off. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 2);

    public static final int MAX_FILL = 2;

    /**
     * One in this many random ticks advances the fill.
     *
     * <p>A block gets a random tick roughly once a minute at the default
     * {@code randomTickSpeed}, so at 3 this is about six minutes from dry to
     * full — long enough to be a trip, short enough to test.
     */
    public static final int FILL_CHANCE = 3;

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(5, 4, 11, 11, 11, 16),
            Direction.SOUTH, Block.box(5, 4, 0, 11, 11, 5),
            Direction.WEST, Block.box(11, 4, 5, 16, 11, 11),
            Direction.EAST, Block.box(0, 4, 5, 5, 11, 11));

    public SapTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    // ---------------------------------------------------------------- placement

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) {
            return null; // you tap the side of a trunk, not the top of a stump
        }
        return defaultBlockState().setValue(FACING, face);
    }

    /** {@link #FACING} points away from the trunk, so the trunk is behind it. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos trunk = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(trunk).is(SigilsBlockTags.TAPPABLE);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);

        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true); // somebody felled the tree out from under it
        }
    }

    // ------------------------------------------------------------------- filling

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int fill = state.getValue(FILL);
        if (fill >= MAX_FILL || random.nextInt(FILL_CHANCE) != 0) {
            return;
        }
        level.setBlock(pos, state.setValue(FILL, fill + 1), Block.UPDATE_CLIENTS);
    }

    // --------------------------------------------------------------- interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(FILL) < MAX_FILL) {
            return InteractionResult.CONSUME; // still running; nothing to take
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack sap = new ItemStack(SigilsItems.SILVERWOOD_SAP.get());
        if (!player.getInventory().add(sap)) {
            player.drop(sap, false);
        }

        level.setBlock(pos, state.setValue(FILL, 0), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.CONSUME;
    }
}