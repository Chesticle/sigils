package com.sigils.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sigils.client.draft.CanvasRenderer;
import com.sigils.client.draft.ClientGlyphs;
import com.sigils.client.draft.DraftSession;
import com.sigils.client.draft.DraftTemplates;
import com.sigils.client.draft.PaletteEntry;
import com.sigils.client.draft.TraceRecorder;
import com.sigils.core.draft.DraftQuality;
import com.sigils.core.draft.GlyphAvailability;
import com.sigils.core.geometry.StrokePath;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.Glyph;
import com.sigils.core.glyph.GlyphInstance;
import com.sigils.core.glyph.GlyphRole;
import com.sigils.draft.DraftContext;
import com.sigils.menu.DraftingTableMenu;
import com.sigils.net.SpellDraftPayload;

/**
 * The drafting table's canvas.
 *
 * <p>Two modes in one screen. While {@code recorder} is null the player is
 * arranging glyphs; once they press Trace it holds a {@link TraceRecorder} and
 * the same canvas becomes something to draw on. Everything the screen shows
 * about what is and isn't allowed comes from {@link DraftContext}, which both
 * sides build from the same items and the same synced registries — so the
 * screen can be wrong about the future but never about the rules.
 */
public class DraftingTableScreen extends AbstractContainerScreen<DraftingTableMenu> {

    // ------------------------------------------------------------------- layout

    public static final int PANEL_WIDTH = 256;
    public static final int PANEL_HEIGHT = 298;

    public static final int CANVAS_X = 30;
    public static final int CANVAS_Y = 14;
    /**
     * 124, not 132. The ink label sits at {@code INK_BAR_Y - 10} = 140, and a
     * 132-pixel canvas would run to 146 and swallow it.
     */
    public static final int CANVAS_SIZE = 124;

    private static final int PALETTE_X = 170;
    private static final int PALETTE_Y = 14;
    private static final int PALETTE_CELL = 24;
    private static final int PALETTE_COLUMNS = 3;

    private static final int TOOLS_X = 8;
    private static final int TOOLS_Y = 3;

    private static final int INK_BAR_X = CANVAS_X;
    private static final int INK_BAR_Y = 150;
    private static final int INK_BAR_WIDTH = 212;
    private static final int INK_BAR_HEIGHT = 6;

    private static final int ERROR_X = 8;
    private static final int ERROR_Y = 160;
    private static final int ERROR_LINE_HEIGHT = 10;
    private static final int ERROR_LINES = 2;

    private static final int BUTTON_Y = 184;
    private static final int BUTTON_HEIGHT = 20;

    // ------------------------------------------------------------------ colours
    // Every colour is ARGB. RGB values render invisible.

    private static final int COLOUR_FRAME = 0xFF241C14;
    private static final int COLOUR_PANEL = 0xFF3E3226;
    private static final int COLOUR_WELL = 0xFF1B140F;
    private static final int COLOUR_PAPER = 0xFFE8DCC0;
    private static final int COLOUR_GUIDE = 0x33000000;
    private static final int COLOUR_SELECTED_CELL = 0x66FFFFFF;
    private static final int COLOUR_CREST = 0xFFB4451E;
    private static final int COLOUR_MODIFIER = 0xFF2E6B72;
    private static final int COLOUR_RING = 0xFF2B2118;
    private static final int COLOUR_HELD = 0x99000000;
    private static final int COLOUR_INK_OVER = 0xFFB4451E;
    private static final int COLOUR_TEXT = 0xFFE8DCC0;
    private static final int COLOUR_ERROR = 0xFFD98C4A;
    private static final int COLOUR_OK = 0xFF8FBF6F;

    // Tracing (Phase 4D)
    private static final int COLOUR_BAND = 0x22000000;
    private static final int COLOUR_GUIDE_ACTIVE = 0x88000000;
    private static final int COLOUR_GUIDE_DONE = 0x22000000;
    private static final int COLOUR_TRACE_DONE = 0xFF5A9E4A;

    // Locked palette entries (Phase 5C)
    private static final int COLOUR_LOCKED_CELL = 0xFF171210;
    private static final int COLOUR_LOCKED_GLYPH = 0xFF4A4038;
    private static final int COLOUR_LOCKED_MARK = 0xFF7A3A2A;
    private static final int COLOUR_SLOT_LOCKED = 0xB0140E0A;

    private static final float STROKE_THICKNESS = 1.4f;

    // -------------------------------------------------------------------- state

    private ClientGlyphs catalogue;
    private DraftSession session;
    private List<PaletteEntry> palette;

    /** Null while arranging; non-null once the player is tracing. */
    private TraceRecorder recorder;

    private Button primaryButton;    // "Trace" then "Inscribe"
    private Button secondaryButton;  // "Turn 15°" then "Redo"

    public DraftingTableScreen(DraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.inventoryLabelY = 208;
    }

    @Override
    protected void init() {
        super.init();

        DraftContext context = menu.context();
        catalogue = ClientGlyphs.snapshot();
        session = new DraftSession(catalogue.lookup(), context.limits());
        palette = catalogue.palette(context.limits(), context.hasPen());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.sigils.drafting.clear"),
                        button -> {
                            recorder = null;
                            session.clear();
                        })
                .bounds(leftPos + 8, topPos + BUTTON_Y, 44, BUTTON_HEIGHT)
                .build());

        secondaryButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sigils.drafting.turn"),
                        button -> {
                            if (recorder == null) {
                                session.rotateHeld(DraftSession.ROTATION_STEP);
                            } else {
                                recorder.redoTarget();
                            }
                        })
                .bounds(leftPos + 56, topPos + BUTTON_Y, 44, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.sigils.drafting.save"),
                        button -> DraftTemplates.save(session.placements()))
                .bounds(leftPos + 104, topPos + BUTTON_Y, 36, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.sigils.drafting.load"),
                        button -> {
                            if (recorder == null) {
                                session.load(DraftTemplates.loadLast());
                            }
                        })
                .bounds(leftPos + 144, topPos + BUTTON_Y, 36, BUTTON_HEIGHT)
                .build());

        primaryButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sigils.drafting.trace"),
                        button -> onPrimary())
                .bounds(leftPos + 184, topPos + BUTTON_Y, 64, BUTTON_HEIGHT)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        DraftContext context = menu.context();

        if (recorder == null) {
            // The pen, ink or paper can change while the screen is open.
            session.limits(context.limits());
            palette = catalogue.palette(context.limits(), context.hasPen());

            primaryButton.setMessage(Component.translatable("screen.sigils.drafting.trace"));
            primaryButton.active = context.ready() && session.validation().valid()
                    && !session.placements().isEmpty();
            secondaryButton.setMessage(Component.translatable("screen.sigils.drafting.turn"));
        } else {
            primaryButton.setMessage(Component.translatable("screen.sigils.drafting.inscribe"));
            primaryButton.active = recorder.complete();
            secondaryButton.setMessage(Component.translatable("screen.sigils.drafting.redo"));
        }
    }

    /** Trace ▸ locks the arrangement; Inscribe ▸ sends it. */
    private void onPrimary() {
        if (recorder == null) {
            // Anything on the cursor was never placed, so it isn't part of the
            // arrangement being traced. Drop it, or it follows the mouse forever.
            session.discardHeld();
            recorder = new TraceRecorder(
                    session.placements(), catalogue.lookup(), menu.context().inkCapacity());
            return;
        }
        if (!recorder.complete()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new SpellDraftPayload(
                menu.containerId,
                SpellDraftPayload.from(recorder.ordered()),
                recorder.encoded()));

        // Optimistic reset. If the server refuses, the parchment simply doesn't
        // change — and the server logs why.
        recorder = null;
        session.clear();
    }

    // ------------------------------------------------------------------ drawing

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

        drawGuides(graphics);
        drawPlacements(graphics);
        drawHeld(graphics, mouseX, mouseY);
        drawPalette(graphics, mouseX, mouseY);
        drawInkBar(graphics);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);

        // Tracing locks the tools; veil them so the screen says so. This has to
        // happen after super, because that's what draws the items — extractLabels
        // runs before the slots, so a veil there would sit underneath the pen.
        if (recorder != null) {
            for (int i = 0; i < DraftingTableMenu.SLOT_COUNT; i++) {
                Slot locked = menu.slots.get(i);
                graphics.fill(locked.x - 1, locked.y - 1, locked.x + 17, locked.y + 17,
                        COLOUR_SLOT_LOCKED);
            }
        }
    }

    /** The centre mark and the boundary the pen allows — the snapping targets. */
    private void drawGuides(GuiGraphicsExtractor graphics) {
        float centreX = canvasX(0.5f);
        float centreY = canvasY(0.5f);
        float radius = session.limits().canvasRadius() * CANVAS_SIZE;

        CanvasRenderer.circle(graphics, centreX, centreY, radius, 48, 1f, COLOUR_GUIDE);
        CanvasRenderer.segment(graphics, centreX - 3, centreY, centreX + 3, centreY, 1f, COLOUR_GUIDE);
        CanvasRenderer.segment(graphics, centreX, centreY - 3, centreX, centreY + 3, 1f, COLOUR_GUIDE);
    }

    private void drawPlacements(GuiGraphicsExtractor graphics) {
        if (recorder == null) {
            for (GlyphInstance placement : session.placements()) {
                catalogue.get(placement.glyphId()).ifPresent(glyph ->
                        CanvasRenderer.placed(graphics, glyph, placement,
                                leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_SIZE,
                                STROKE_THICKNESS, colourFor(glyph.role())));
            }
            return;
        }
        drawTraceGuides(graphics);
        drawTraceLines(graphics);
    }

    /** Faint ideal geometry, with a tolerance band around the glyph being drawn. */
    private void drawTraceGuides(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < recorder.size(); i++) {
            List<StrokePath> ideal = recorder.idealAt(i);
            boolean current = i == recorder.target();

            if (current) {
                // The band you have to stay inside, drawn as a fat translucent line.
                float band = recorder.toleranceAt(i) * 2f * CANVAS_SIZE;
                CanvasRenderer.strokes(graphics, ideal,
                        leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_SIZE, band, COLOUR_BAND);
            }

            CanvasRenderer.strokes(graphics, ideal,
                    leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_SIZE, 1f,
                    current ? COLOUR_GUIDE_ACTIVE
                            : recorder.donePast(i) ? COLOUR_GUIDE_DONE : COLOUR_GUIDE);
        }
    }

    /** The player's own line, green where it's accurate and red where it isn't. */
    private void drawTraceLines(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < recorder.size(); i++) {
            List<Vec2> points = recorder.samplesAt(i);
            float tolerance = recorder.toleranceAt(i);
            for (int p = 1; p < points.size(); p++) {
                Vec2 a = points.get(p - 1);
                Vec2 b = points.get(p);
                int colour = recorder.donePast(i)
                        ? COLOUR_TRACE_DONE
                        : accuracyColour(recorder.deviationAt(b), tolerance);
                CanvasRenderer.segment(graphics,
                        canvasX(a.x()), canvasY(a.y()),
                        canvasX(b.x()), canvasY(b.y()),
                        2f, colour);
            }
        }
    }

    /** Green inside the band, fading to red as the pen strays past it. */
    private static int accuracyColour(float deviation, float tolerance) {
        float t = tolerance <= 0f ? 1f : Math.clamp(deviation / tolerance, 0f, 1f);
        int red = Math.round(0x5A + t * (0xC8 - 0x5A));
        int green = Math.round(0xC8 - t * (0xC8 - 0x3A));
        return 0xFF000000 | (red << 16) | (green << 8) | 0x30;
    }

    /** The glyph on the cursor, previewed where it would land if dropped. */
    private void drawHeld(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (recorder != null) {
            return;
        }
        session.held().ifPresent(held -> {
            if (!inCanvas(mouseX, mouseY)) {
                return;
            }
            catalogue.get(held.glyphId()).ifPresent(glyph -> {
                GlyphInstance preview = new GlyphInstance(
                        held.glyphId(), canvasPoint(mouseX, mouseY), held.rotation(), held.scale());
                CanvasRenderer.placed(graphics, glyph, preview,
                        leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_SIZE,
                        STROKE_THICKNESS, COLOUR_HELD);
            });
        });
    }

    private void drawPalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < palette.size(); i++) {
            PaletteEntry entry = palette.get(i);
            Glyph glyph = entry.glyph();

            int cellX = leftPos + PALETTE_X + (i % PALETTE_COLUMNS) * PALETTE_CELL;
            int cellY = topPos + PALETTE_Y + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            int cellSize = PALETTE_CELL - 2;
            boolean hovered = mouseX >= cellX && mouseX < cellX + cellSize
                    && mouseY >= cellY && mouseY < cellY + cellSize;

            graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize,
                    entry.locked() ? COLOUR_LOCKED_CELL : COLOUR_WELL);

            // Hover feedback, but only where clicking would do something.
            if (hovered && !entry.locked()) {
                graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, COLOUR_SELECTED_CELL);
            }
            if (hovered && entry.locked()) {
                graphics.outline(cellX, cellY, cellSize, cellSize, COLOUR_LOCKED_MARK);
            }

            // The glyph's own strokes, drawn straight into the cell. This loop is
            // the entire reason a new glyph needs no UI code.
            CanvasRenderer.strokes(graphics, glyph.strokes(),
                    cellX + 3, cellY + 3, PALETTE_CELL - 8,
                    1.2f, entry.locked() ? COLOUR_LOCKED_GLYPH : colourFor(glyph.role()));

            if (entry.locked()) {
                CanvasRenderer.segment(graphics,
                        cellX + 4, cellY + 4, cellX + cellSize - 4, cellY + cellSize - 4,
                        1.2f, COLOUR_LOCKED_MARK);
            }

            if (hovered) {
                setPaletteTooltip(graphics, entry, mouseX, mouseY);
            }
        }
    }

    /**
     * What this glyph is, and — if it's locked — what would unlock it.
     *
     * <p>Submitted from {@code extractBackground} on purpose: the tooltip is
     * drawn after the panel's pose is popped, so it needs absolute coordinates,
     * and the mouse position here is absolute too. Doing this from
     * {@code extractLabels} puts the tooltip a panel's width away from the
     * cursor.
     */
    private void setPaletteTooltip(GuiGraphicsExtractor graphics, PaletteEntry entry,
                                   int mouseX, int mouseY) {
        Glyph glyph = entry.glyph();
        List<Component> lines = new ArrayList<>(3);

        // "sigils:crest_fire" -> "glyph.sigils.crest_fire", so a datapack-added
        // glyph gets a proper name from one lang key and no code.
        lines.add(Component.translatable("glyph." + glyph.id().replace(':', '.'))
                .withStyle(entry.locked() ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));

        lines.add(Component.translatable("screen.sigils.drafting.glyph_detail",
                        Component.translatable("glyph_role.sigils."
                                + glyph.role().name().toLowerCase(Locale.ROOT)),
                        glyph.complexity(),
                        String.format("%.1f", glyph.inkCost()))
                .withStyle(ChatFormatting.GRAY));

        if (entry.locked()) {
            lines.add(lockReason(entry.availability()).withStyle(ChatFormatting.GOLD));
        }

        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    /** The data in a {@link GlyphAvailability}, turned into a sentence. */
    private static MutableComponent lockReason(GlyphAvailability availability) {
        return switch (availability.reason()) {
            case NO_PEN -> Component.translatable("screen.sigils.drafting.locked_no_pen");
            case TOO_COMPLEX -> Component.translatable(
                    "screen.sigils.drafting.locked_complexity",
                    availability.required(), availability.available());
            case NOT_LEARNED -> Component.translatable("screen.sigils.drafting.locked_unknown");
            case AVAILABLE -> Component.empty();
        };
    }

    private void drawInkBar(GuiGraphicsExtractor graphics) {
        DraftContext context = menu.context();
        int x = leftPos + INK_BAR_X;
        int y = topPos + INK_BAR_Y;

        graphics.fill(x, y, x + INK_BAR_WIDTH, y + INK_BAR_HEIGHT, COLOUR_WELL);

        float capacity = context.inkCapacity();
        if (capacity <= 0f) {
            return;
        }

        // The outline carries the grade's colour even at zero spend, so swapping
        // ink is visible without placing a glyph first.
        graphics.outline(x, y, INK_BAR_WIDTH, INK_BAR_HEIGHT, context.inkTint());

        float spent = recorder == null ? session.inkCost() : recorder.ledger().spent();
        float fraction = Math.clamp(spent / capacity, 0f, 1f);
        boolean over = spent > capacity;

        graphics.fill(x + 1, y + 1,
                x + 1 + Math.round(fraction * (INK_BAR_WIDTH - 2)), y + INK_BAR_HEIGHT - 1,
                over ? COLOUR_INK_OVER : context.inkTint());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Deliberately not calling super. It would draw the block's name across
        // the tools line, and the tools line says strictly more. The inventory
        // label is redrawn below because that one is worth keeping.
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOUR_TEXT, false);

        DraftContext context = menu.context();

        // What you're holding, and what it permits. Panel-relative coordinates:
        // extractLabels draws inside the translated pose.
        Component tools = context.ready()
                ? Component.translatable("screen.sigils.drafting.tools",
                menu.pen().getHoverName(),
                menu.parchment().getHoverName(),
                context.limits().maxComplexity(),
                context.limits().maxCrests())
                : Component.translatable("screen.sigils.drafting.missing",
                String.join(", ", context.missing()));
        graphics.text(font, tools, TOOLS_X, TOOLS_Y,
                context.ready() ? COLOUR_TEXT : COLOUR_ERROR, false);

        // Ink, above its bar, named by grade when one is loaded.
        float spent = recorder == null ? session.inkCost() : recorder.ledger().spent();
        Component inkLine = context.inkGrade()
                .map(grade -> (Component) Component.translatable("screen.sigils.drafting.ink_graded",
                        String.format("%.1f", spent),
                        String.format("%.1f", context.inkCapacity()),
                        Component.translatable("ink_grade." + grade.id().replace(':', '.'))))
                .orElseGet(() -> Component.translatable("screen.sigils.drafting.ink",
                        String.format("%.1f", spent),
                        String.format("%.1f", context.inkCapacity())));
        graphics.text(font, inkLine, INK_BAR_X, INK_BAR_Y - 10,
                spent > context.inkCapacity() ? COLOUR_ERROR : COLOUR_TEXT, false);

        // While tracing: progress, then what the tools will actually record.
        if (recorder != null) {
            Component progress = recorder.complete()
                    ? Component.translatable("screen.sigils.drafting.traced")
                    : Component.translatable("screen.sigils.drafting.progress",
                    recorder.target() + 1, recorder.size(),
                    Math.round(recorder.currentCoverage() * 100));
            graphics.text(font, progress, ERROR_X, ERROR_Y,
                    recorder.complete() ? COLOUR_OK : COLOUR_TEXT, false);

            if (!recorder.message().isEmpty()) {
                graphics.text(font, Component.literal(recorder.message()),
                        ERROR_X, ERROR_Y + ERROR_LINE_HEIGHT, COLOUR_ERROR, false);
            } else if (recorder.complete()) {
                float hand = recorder.meanFidelity();
                float recorded = DraftQuality.effectiveFidelity(
                        hand, context.pen(), context.parchmentQuality());

                graphics.text(font, Component.translatable(
                                "screen.sigils.drafting.fidelity_preview",
                                String.format("%.2f", hand),
                                String.format("%.2f", recorded)),
                        ERROR_X, ERROR_Y + ERROR_LINE_HEIGHT,
                        recorded < hand ? COLOUR_ERROR : COLOUR_OK, false);
            }
            return;
        }

        // What's stopping this from being a spell.
        if (!context.ready()) {
            return; // the tools line already named what's missing
        }

        List<String> errors = session.validation().errors();
        if (errors.isEmpty()) {
            graphics.text(font, Component.translatable("screen.sigils.drafting.valid"),
                    ERROR_X, ERROR_Y, COLOUR_OK, false);
            graphics.text(font, Component.translatable("screen.sigils.drafting.trace_next"),
                    ERROR_X, ERROR_Y + ERROR_LINE_HEIGHT, COLOUR_TEXT, false);
            return;
        }

        int shown = Math.min(errors.size(), ERROR_LINES);
        for (int i = 0; i < shown; i++) {
            graphics.text(font, Component.literal(errors.get(i)),
                    ERROR_X, ERROR_Y + i * ERROR_LINE_HEIGHT, COLOUR_ERROR, false);
        }
        if (errors.size() > shown) {
            graphics.text(font, Component.translatable("screen.sigils.drafting.more_errors",
                            errors.size() - shown),
                    ERROR_X, ERROR_Y + shown * ERROR_LINE_HEIGHT, COLOUR_ERROR, false);
        }
    }

    // -------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (recorder != null) {
            if (inCanvas(mouseX, mouseY)) {
                recorder.penDown(canvasPoint(mouseX, mouseY));
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        int paletteIndex = paletteIndexAt(mouseX, mouseY);
        if (paletteIndex >= 0) {
            PaletteEntry entry = palette.get(paletteIndex);
            if (!entry.locked()) {
                session.take(entry.glyph());
            }
            // Swallowed either way: letting a click on a locked cell fall through
            // would make something unrelated happen, which teaches the wrong thing.
            return true;
        }

        if (inCanvas(mouseX, mouseY)) {
            Vec2 point = canvasPoint(mouseX, mouseY);
            if (session.held().isPresent()) {
                session.place(point);
            } else {
                int index = session.indexAt(point);
                if (index >= 0) {
                    session.lift(index);
                }
            }
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (recorder != null && inCanvas(event.x(), event.y())) {
            recorder.penMove(canvasPoint(event.x(), event.y()));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (recorder != null) {
            recorder.penUp();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        // The trace is being scored against the tools that were in the table when
        // it started. Letting them change halfway would make the fidelity preview
        // a lie and the ink ledger meaningless. Blocking every slot rather than
        // just the table's three is deliberate: a shift-click from the backpack
        // reaches the table without ever touching a table slot.
        if (recorder != null) {
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (recorder == null && session.held().isPresent()
                && (inCanvas(mouseX, mouseY) || paletteIndexAt(mouseX, mouseY) >= 0)) {
            session.scaleHeld(scrollY > 0 ? 1.1f : 1f / 1.1f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------ helpers

    private float canvasX(float u) {
        return leftPos + CANVAS_X + u * CANVAS_SIZE;
    }

    private float canvasY(float v) {
        return topPos + CANVAS_Y + v * CANVAS_SIZE;
    }

    private Vec2 canvasPoint(double mouseX, double mouseY) {
        return new Vec2(
                (float) (mouseX - leftPos - CANVAS_X) / CANVAS_SIZE,
                (float) (mouseY - topPos - CANVAS_Y) / CANVAS_SIZE);
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        double x = mouseX - leftPos - CANVAS_X;
        double y = mouseY - topPos - CANVAS_Y;
        return x >= 0 && x < CANVAS_SIZE && y >= 0 && y < CANVAS_SIZE;
    }

    private int paletteIndexAt(double mouseX, double mouseY) {
        int localX = (int) (mouseX - leftPos - PALETTE_X);
        int localY = (int) (mouseY - topPos - PALETTE_Y);
        if (localX < 0 || localY < 0) {
            return -1;
        }
        int column = localX / PALETTE_CELL;
        int row = localY / PALETTE_CELL;
        if (column >= PALETTE_COLUMNS) {
            return -1;
        }
        int index = row * PALETTE_COLUMNS + column;
        return index < palette.size() ? index : -1;
    }

    private static int colourFor(GlyphRole role) {
        return switch (role) {
            case CREST -> COLOUR_CREST;
            case MODIFIER -> COLOUR_MODIFIER;
            default -> COLOUR_RING;
        };
    }
}
