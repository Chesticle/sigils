package com.sigils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

import com.sigils.circuit.Circuits;

/**
 * A spell drawn onto the face of a block.
 *
 * <p>Placement, survival and shape follow glow lichen: the sigil occupies the
 * empty cell <em>next to</em> the block it is drawn on, and {@link #FACING} points
 * away from that support. A floor sigil faces {@link Direction#UP}.
 *
 * <p><b>There is deliberately no {@code BlockItem}.</b> You never hold one of
 * these; a sigil comes into existence only by pressing an inscribed parchment
 * against a surface, which is why it isn't in the creative tab and why its loot
 * table is empty.
 */
public class WorldSigilBlock extends BaseEntityBlock {

    public static final MapCodec<WorldSigilBlock> CODEC = simpleCodec(WorldSigilBlock::new);

    /** The direction the drawn surface points — away from the block it's drawn on. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    /** True for the moment after firing. Drives the light level, nothing else. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** How long the mark glows after casting. */
    public static final int FLASH_TICKS = 10;

    /** A one-pixel skin against the support face, in each of six orientations. */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, Block.box(0, 0, 0, 16, 1, 16),
            Direction.DOWN, Block.box(0, 15, 0, 16, 16, 16),
            Direction.NORTH, Block.box(0, 0, 15, 16, 16, 16),
            Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 1),
            Direction.EAST, Block.box(0, 0, 0, 1, 16, 16),
            Direction.WEST, Block.box(15, 0, 0, 16, 16, 16));

    public WorldSigilBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.UP)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorldSigilBlockEntity(pos, state);
    }

    // ----------------------------------------------------------------- geometry

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Nothing walks on a drawing. Also what lets a pressure plate sit beside one. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    // ------------------------------------------------------------------- ticking

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {

        if (level.isClientSide()) {
            return null; // the client has nothing to decide
        }
        return createTickerHelper(type, SigilsBlocks.WORLD_SIGIL_ENTITY.get(),
                WorldSigilBlockEntity::serverTick);
    }

    // --------------------------------------------------------------- world hooks

    /** Scheduled by the block entity when it fires. Puts the light back out. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);

        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, false); // the wall it was drawn on is gone
            return;
        }
        if (level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil
                && level instanceof ServerLevel server) {
            sigil.onNeighborChanged(server);
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil)
                || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        Identifier trigger = sigil.cycleTrigger(server);
        player.sendSystemMessage(Component.translatable("message.sigils.sigil.trigger",
                Component.translatable(Circuits.descriptionKey(trigger)),
                Component.translatable(Circuits.descriptionKey(trigger) + ".hint")));
        return InteractionResult.CONSUME;
    }
}