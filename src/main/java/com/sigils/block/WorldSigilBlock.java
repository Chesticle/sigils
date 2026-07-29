package com.sigils.block;

import com.mojang.serialization.MapCodec;
import com.sigils.core.sigil.SigilIntegrity;
import com.sigils.item.SigilsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

import com.sigils.circuit.Circuits;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import com.sigils.registry.SigilsPens;

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

    /**
     * How worn the drawing looks: 0 pristine, {@link SigilIntegrity#WEAR_STEPS} spent.
     *
     * <p>In the block state rather than only in the block entity, and that is the
     * whole performance story of this part. The renderer reacts to block state
     * changes for free; it does not react to a block entity field moving. Five
     * buckets means a sigil's entire life is four visible transitions, so rain
     * that would otherwise send fifty packets sends four.
     */
    public static final IntegerProperty WEAR =
            IntegerProperty.create("wear", 0, SigilIntegrity.WEAR_STEPS);

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
                .setValue(LIT, false)
                .setValue(WEAR, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, WEAR);
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

    /**
     * Water wears it, a sponge kills it, a pen peels it off.
     *
     * <p>Three tools, one method, and a {@code PASS} for everything else so that
     * holding a torch and right-clicking still places the torch.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {

        boolean bucket = stack.is(Items.WATER_BUCKET);
        boolean sponge = stack.is(Items.SPONGE) || stack.is(Items.WET_SPONGE);
        boolean pen = !level.isClientSide()
                && SigilsPens.table(level.registryAccess()).containsKey(stack.getItem());

        if (!bucket && !sponge && !pen) {
            // NOT InteractionResult.PASS. useItemOn runs before useWithoutItem and
            // gates it: only TRY_WITH_EMPTY_HAND hands control on. PASS means
            // "handled, nothing happened", which silently eats the empty-hand
            // interaction — and trigger cycling with it.
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil)
                || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        if (bucket) {
            sigil.setIntegrity(sigil.integrity().washed(SigilIntegrity.WASH_BUCKET));
            if (!player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            splash(server, pos, 14);
            server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.CONSUME;
        }

        if (sponge) {
            sigil.setIntegrity(sigil.integrity().washed(SigilIntegrity.WASH_SPONGE));
            splash(server, pos, 24);
            server.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 0.9f, 1.1f);
            return InteractionResult.CONSUME;
        }

        return peel(sigil, server, pos, player);
    }

    /**
     * Scrape the sigil off with a nib.
     *
     * <p>Three outcomes, because there are three integrity bands and each of them
     * means something different about the sheet underneath.
     */
    private static InteractionResult peel(WorldSigilBlockEntity sigil, ServerLevel level,
                                          BlockPos pos, Player player) {
        SigilIntegrity integrity = sigil.integrity();

        ItemStack recovered;
        String message;
        if (integrity.intact()) {
            recovered = sigil.recoverSheet();
            message = "message.sigils.sigil.peeled_clean";
        } else if (!integrity.inert()) {
            recovered = new ItemStack(SigilsItems.PARCHMENT.get());
            message = "message.sigils.sigil.peeled_spoiled";
        } else {
            recovered = ItemStack.EMPTY;
            message = "message.sigils.sigil.peeled_nothing";
        }

        level.removeBlock(pos, false);
        if (!recovered.isEmpty() && !player.addItem(recovered)) {
            player.drop(recovered, false);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.sendSystemMessage(Component.translatable(message));
        return InteractionResult.CONSUME;
    }

    private static void splash(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, 0.35, 0.3, 0.35, 0.08);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof WorldSigilBlockEntity sigil)
                || !(level instanceof ServerLevel server)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        Identifier trigger = sigil.cycleTrigger(server);
        player.sendSystemMessage(Component.translatable("message.sigils.sigil.trigger",
                Component.translatable(Circuits.descriptionKey(trigger)),
                Component.translatable(Circuits.descriptionKey(trigger) + ".hint")));
        return InteractionResult.CONSUME;
    }
}