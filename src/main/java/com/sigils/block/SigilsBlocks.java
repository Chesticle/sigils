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

    public static final Supplier<BlockEntityType<DraftingTableBlockEntity>> DRAFTING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("drafting_table", () -> new BlockEntityType<>(
                    DraftingTableBlockEntity::new,
                    false,                      // OP-only NBT loading
                    DRAFTING_TABLE.get()));     // vararg of valid blocks

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}