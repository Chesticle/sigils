package com.sigils.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.sigils.draft.DraftContext;
import com.sigils.menu.DraftingTableMenu;

public class DraftingTableScreen extends AbstractContainerScreen<DraftingTableMenu> {

    public static final int PANEL_WIDTH = 256;
    public static final int PANEL_HEIGHT = 240;

    public static final int CANVAS_X = 30;
    public static final int CANVAS_Y = 12;
    public static final int CANVAS_SIZE = 140;

    private static final int COLOUR_FRAME = 0xFF241C14;
    private static final int COLOUR_PANEL = 0xFF3E3226;
    private static final int COLOUR_WELL = 0xFF1B140F;
    private static final int COLOUR_PAPER = 0xFFE8DCC0;
    private static final int COLOUR_TEXT = 0xFFE8DCC0;
    private static final int COLOUR_WARN = 0xFFD98C4A;

    public DraftingTableScreen(DraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.inventoryLabelY = PANEL_HEIGHT - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOUR_FRAME);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, COLOUR_PANEL);

        graphics.fill(x + CANVAS_X - 2, y + CANVAS_Y - 2,
                x + CANVAS_X + CANVAS_SIZE + 2, y + CANVAS_Y + CANVAS_SIZE + 2, COLOUR_WELL);
        graphics.fill(x + CANVAS_X, y + CANVAS_Y,
                x + CANVAS_X + CANVAS_SIZE, y + CANVAS_Y + CANVAS_SIZE, COLOUR_PAPER);

        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1,
                    x + slot.x + 17, y + slot.y + 17, COLOUR_WELL);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        DraftContext context = menu.context();
        Component status = context.ready()
                ? Component.translatable("screen.sigils.drafting.ready",
                context.limits().maxGlyphs(),
                context.limits().maxCrests(),
                String.format("%.1f", context.inkCapacity()))
                : Component.translatable("screen.sigils.drafting.missing",
                String.join(", ", context.missing()));

        graphics.text(font, status, CANVAS_X, CANVAS_Y + CANVAS_SIZE + 6,
                context.ready() ? COLOUR_TEXT : COLOUR_WARN, false);
    }
}