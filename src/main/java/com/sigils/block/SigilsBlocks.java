package com.sigils.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.material.PushReaction;

import java.util.Optional;

import com.sigils.worldgen.SigilsFeatures;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;

/** The mod's blocks and block entity types. */
public final class SigilsBlocks {

    private SigilsBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Sigils.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Sigils.MOD_ID);

    /**
     * What a sapling turns into. Named once, here, so the block and the datapack
     * can't drift apart.
     */
    public static final TreeGrower SILVERWOOD_GROWER = new TreeGrower(
            "sigils:silverwood",
            Optional.empty(),                             // no mega variant
            Optional.of(SigilsFeatures.SILVERWOOD_TREE),  // the ordinary tree
            Optional.empty());                            // no flowering variant

    /** Pale, straight, and the only thing a tap will stick to. */
    public static final DeferredBlock<RotatedPillarBlock> SILVERWOOD_LOG = BLOCKS.registerBlock(
            "silverwood_log",
            RotatedPillarBlock::new,
            props -> props
                    .mapColor(MapColor.QUARTZ)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    public static final DeferredBlock<SilverwoodLeavesBlock> SILVERWOOD_LEAVES = BLOCKS.registerBlock(
            "silverwood_leaves",
            SilverwoodLeavesBlock::new,
            props -> props
                    .mapColor(MapColor.PLANT)
                    .strength(0.2f)
                    .randomTicks()
                    .sound(SoundType.CHERRY_LEAVES)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<SaplingBlock> SILVERWOOD_SAPLING = BLOCKS.registerBlock(
            "silverwood_sapling",
            props -> new SaplingBlock(SILVERWOOD_GROWER, props),
            props -> props
                    .noCollision()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<SapTapBlock> SAP_TAP = BLOCKS.registerBlock(
            "sap_tap",
            SapTapBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .randomTicks()
                    .noOcclusion()
                    .sound(SoundType.COPPER));

    public static final DeferredBlock<Block> DRAFTING_TABLE = BLOCKS.registerBlock(
            "drafting_table",
            DraftingTableBlock::new,
            props -> props.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));

    /**
     * A spell drawn on a surface. No block item — see {@link WorldSigilBlock}.
     *
     * <p>{@code noCollission} keeps it out of the way of the pressure plate that
     * triggers it; {@code noOcclusion} stops it darkening the block it's drawn on;
     * the faint light level is so you can find one at night.
     */
    public static final DeferredBlock<WorldSigilBlock> WORLD_SIGIL = BLOCKS.registerBlock(
            "world_sigil",
            WorldSigilBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2f)
                    .noCollision()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(WorldSigilBlock.LIT) ? 12 : 0)
                    .sound(SoundType.VINE));

    public static final Supplier<BlockEntityType<DraftingTableBlockEntity>> DRAFTING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("drafting_table", () -> new BlockEntityType<>(
                    DraftingTableBlockEntity::new,
                    false,                      // OP-only NBT loading
                    DRAFTING_TABLE.get()));     // vararg of valid blocks

    /** The piston-like printer. Full cube, so its face is sturdy enough to draw on. */
    public static final DeferredBlock<SpellPressBlock> SPELL_PRESS = BLOCKS.registerBlock(
            "spell_press",
            SpellPressBlock::new,
            props -> props
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final Supplier<BlockEntityType<WorldSigilBlockEntity>> WORLD_SIGIL_ENTITY =
            BLOCK_ENTITIES.register("world_sigil", () -> new BlockEntityType<>(
                    WorldSigilBlockEntity::new,
                    false,
                    WORLD_SIGIL.get()));

    public static final Supplier<BlockEntityType<SpellPressBlockEntity>> SPELL_PRESS_ENTITY =
            BLOCK_ENTITIES.register("spell_press", () -> new BlockEntityType<>(
                    SpellPressBlockEntity::new,
                    false,
                    SPELL_PRESS.get()));

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}