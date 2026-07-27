package com.sigils.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.sigils.block.DraftingTableBlockEntity;
import com.sigils.block.SigilsBlocks;
import com.sigils.draft.DraftContext;
import com.sigils.draft.InkSupply;
import com.sigils.draft.PenTiers;
import com.sigils.item.SigilsItems;

/**
 * The drafting table's container: parchment, pen, ink, plus the player's
 * inventory.
 *
 * <p>The canvas itself is not in here. What <em>is</em> here is everything the
 * canvas needs to know — and because slot contents sync for free, the client
 * derives the same {@link DraftContext} the server will check against.
 */
public class DraftingTableMenu extends AbstractContainerMenu {

    public static final int SLOT_PARCHMENT = DraftingTableBlockEntity.SLOT_PARCHMENT;
    public static final int SLOT_PEN = DraftingTableBlockEntity.SLOT_PEN;
    public static final int SLOT_INK = DraftingTableBlockEntity.SLOT_INK;
    public static final int SLOT_COUNT = DraftingTableBlockEntity.SLOT_COUNT;

    /** Slot layout, in GUI pixels from the panel's top-left. Part C draws around these. */
    private static final int TABLE_SLOT_X = 8;
    private static final int TABLE_SLOT_Y = 14;
    private static final int TABLE_SLOT_SPACING = 24;
    private static final int INVENTORY_X = 48;
    private static final int INVENTORY_Y = 218;
    private static final int HOTBAR_Y = 276;

    private final Container table;
    private final ContainerLevelAccess access;

    /** Client-side constructor — the {@link SigilsMenus#DRAFTING_TABLE} type calls this. */
    public DraftingTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    /** Server-side constructor — the block entity calls this with itself. */
    public DraftingTableMenu(int containerId, Inventory playerInventory,
                             Container table, ContainerLevelAccess access) {
        super(SigilsMenus.DRAFTING_TABLE.get(), containerId);
        checkContainerSize(table, SLOT_COUNT);
        this.table = table;
        this.access = access;
        table.startOpen(playerInventory.player);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            addSlot(new TableSlot(table, slot, TABLE_SLOT_X, TABLE_SLOT_Y + slot * TABLE_SLOT_SPACING));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    /**
     * Which items each table slot accepts. Static so the block entity, the slot,
     * and Part D's server-side check all ask the same question.
     */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_PARCHMENT -> stack.is(SigilsItems.PARCHMENT.get());
            case SLOT_PEN -> PenTiers.isPen(stack);
            case SLOT_INK -> InkSupply.isInk(stack);
            default -> false;
        };
    }

    public ItemStack parchment() {
        return table.getItem(SLOT_PARCHMENT);
    }

    public ItemStack pen() {
        return table.getItem(SLOT_PEN);
    }

    public ItemStack ink() {
        return table.getItem(SLOT_INK);
    }

    /** The rules in force right now, from the items currently in the table. */
    public DraftContext context() {
        return DraftContext.of(parchment(), pen(), ink());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < SLOT_COUNT) {
            // Table -> player inventory.
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            // Player inventory -> table (filtered slots reject what doesn't belong).
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).is(SigilsBlocks.DRAFTING_TABLE.get())
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        table.stopOpen(player);
    }

    /** A slot that enforces {@link #accepts(int, ItemStack)} on both sides. */
    private static final class TableSlot extends Slot {

        private final int tableIndex;

        private TableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.tableIndex = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return accepts(tableIndex, stack);
        }
    }
}