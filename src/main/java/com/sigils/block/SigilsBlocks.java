package com.sigils.block;

import net.minecraft.core.registries.Registries;
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