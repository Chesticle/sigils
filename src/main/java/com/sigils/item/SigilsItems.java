package com.sigils.item;

import com.sigils.registry.SigilsGlyphs;
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

    /** Finer stock. Holds a traced line better; see {@code sigils:parchment_grade}. */
    public static final DeferredItem<ParchmentItem> VELLUM =
            ITEMS.registerItem("vellum", ParchmentItem::new, props -> props.stacksTo(16));

    /** What you draft with. Each pen's capabilities come from {@code sigils:pen_tier}. */
    public static final DeferredItem<Item> PEN =
            ITEMS.registerSimpleItem("pen", props -> props.stacksTo(1));

    public static final DeferredItem<Item> IRON_PEN =
            ITEMS.registerSimpleItem("iron_pen", props -> props.stacksTo(1));

    public static final DeferredItem<Item> DIAMOND_PEN =
            ITEMS.registerSimpleItem("diamond_pen", props -> props.stacksTo(1));

    public static final DeferredItem<Item> NETHERITE_PEN =
            ITEMS.registerSimpleItem("netherite_pen", props -> props.stacksTo(1));

    /** No recipe. The condensation ritual in Phase 9 is the only source. */
    public static final DeferredItem<Item> FORBIDDEN_PEN =
            ITEMS.registerSimpleItem("forbidden_pen", props -> props.stacksTo(1));

    public static final DeferredItem<Item> MAGICAL_INK =
            ITEMS.registerSimpleItem("magical_ink", props -> props.stacksTo(16));

    /** Tapped from a silverwood tree. Phase 7 grows the trees; until then, craft it. */
    public static final DeferredItem<Item> SILVERWOOD_SAP =
            ITEMS.registerSimpleItem("silverwood_sap", props -> props.stacksTo(16));

    /** Worth more per item, and permanent — the sketchbook gate in Part D. */
    public static final DeferredItem<Item> NETHERITE_INK =
            ITEMS.registerSimpleItem("netherite_ink", props -> props.stacksTo(16));

    /** One glyph, carved in stone. Found, never crafted. */
    public static final DeferredItem<GlyphTabletItem> GLYPH_TABLET =
            ITEMS.registerItem("glyph_tablet", GlyphTabletItem::new, props -> props.stacksTo(16));

    /** A book of finished spells. Only takes permanent ink. */
    public static final DeferredItem<SketchbookItem> SKETCHBOOK =
            ITEMS.registerItem("sketchbook", SketchbookItem::new, props -> props.stacksTo(1));

    /** Holds up to {@link InkVialItem#CAPACITY} units. Drawn down, not consumed. */
    public static final DeferredItem<InkVialItem> INK_VIAL =
            ITEMS.registerItem("ink_vial", InkVialItem::new, props -> props.stacksTo(1));

    public static final DeferredItem<BlockItem> DRAFTING_TABLE =   // BlockItem, not Item
            ITEMS.registerSimpleBlockItem(SigilsBlocks.DRAFTING_TABLE);

    public static final DeferredItem<BlockItem> SPELL_PRESS =
            ITEMS.registerSimpleBlockItem(SigilsBlocks.SPELL_PRESS);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("sigils", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sigils"))
            .icon(() -> new ItemStack(PARCHMENT.get()))
            .displayItems((parameters, output) -> {
                output.accept(DRAFTING_TABLE.get());
                output.accept(SPELL_PRESS.get());
                output.accept(PARCHMENT.get());
                output.accept(VELLUM.get());
                output.accept(PEN.get());
                output.accept(IRON_PEN.get());
                output.accept(DIAMOND_PEN.get());
                output.accept(NETHERITE_PEN.get());
                output.accept(FORBIDDEN_PEN.get());
                output.accept(SILVERWOOD_SAP.get());
                output.accept(MAGICAL_INK.get());
                output.accept(NETHERITE_INK.get());
                output.accept(INK_VIAL.get());
                output.accept(SKETCHBOOK.get());
                // One tablet per glyph, straight off the registry. A datapack
                // that adds a glyph gets a tablet here with no code written for
                // it — the same test the palette passes, applied to an item.
                for (String glyphId : SigilsGlyphs.ids(parameters.holders())) {
                    output.accept(GlyphTabletItem.of(glyphId));
                }
            })
            .build());

    /** Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}