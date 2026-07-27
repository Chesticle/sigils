package com.sigils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.sigils.menu.DraftingTableMenu;

/**
 * Holds the three things a draft needs: something to write on, something to
 * write with, and something to write in.
 *
 * <p>It does <em>not</em> hold the draft. Placements and strokes live on the
 * client until the player confirms, at which point one packet carries the lot
 * (Part D). A block entity that tried to mirror an in-progress drawing would be
 * a stream of packets for no gain.
 */
public class DraftingTableBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SLOT_PARCHMENT = 0;
    public static final int SLOT_PEN = 1;
    public static final int SLOT_INK = 2;
    public static final int SLOT_COUNT = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public DraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(SigilsBlocks.DRAFTING_TABLE_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- Container

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return DraftingTableMenu.accepts(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // ------------------------------------------------------------- MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sigils.drafting_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DraftingTableMenu(containerId, playerInventory, this,
                ContainerLevelAccess.create(level, worldPosition));
    }

    // -------------------------------------------------------------- Persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}