package com.sigils.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.sigils.Sigils;
import com.sigils.block.SigilsBlocks;

/**
 * The mod's items, and the creative tab they live in.
 *
 * <p>All three are Phase 4 placeholders in the sense that Phase 5 gives them
 * recipes, tiers and gating — but they are <em>real</em> items, not debug ones.
 * Nothing about them is thrown away later.
 */
public final class SigilsItems {

    private SigilsItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Sigils.MOD_ID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Sigils.MOD_ID);

    public static final DeferredItem<ParchmentItem> PARCHMENT =
            ITEMS.registerItem("parchment", ParchmentItem::new, props -> props.stacksTo(16));

    public static final DeferredItem<Item> PEN =
            ITEMS.registerSimpleItem("pen", props -> props.stacksTo(1));

    public static final DeferredItem<Item> MAGICAL_INK =
            ITEMS.registerSimpleItem("magical_ink", props -> props.stacksTo(16));

    public static final DeferredItem<BlockItem> DRAFTING_TABLE =   // BlockItem, not Item
            ITEMS.registerSimpleBlockItem(SigilsBlocks.DRAFTING_TABLE);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("sigils", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sigils"))
            .icon(() -> new ItemStack(PARCHMENT.get()))
            .displayItems((parameters, output) -> {
                output.accept(DRAFTING_TABLE.get());
                output.accept(PARCHMENT.get());
                output.accept(PEN.get());
                output.accept(MAGICAL_INK.get());
            })
            .build());

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}