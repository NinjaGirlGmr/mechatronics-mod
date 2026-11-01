package com.gamer.mechatronics.client;

// ---- Imports: classes from other packages this file uses ----
import com.gamer.mechatronics.block.BreadboardBlockEntity;                 // the board's data (its wires)
import com.gamer.mechatronics.block.Wire;                                  // one wire: two holes + a color
import com.gamer.mechatronics.menu.BreadboardMenu;                         // our menu (holds columns, strips, pos)
import com.gamer.mechatronics.network.AddWirePayload;                      // the packet that asks the server to add a wire
import net.minecraft.client.gui.GuiGraphicsExtractor;                      // the "paintbrush" for drawing on screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen; // vanilla's base class for menu windows
import net.minecraft.client.input.MouseButtonEvent;                        // a mouse click: where, and which button
import net.minecraft.network.chat.Component;                               // text that can be shown in game
import net.minecraft.world.entity.player.Inventory;                        // the player's inventory (passed in by the game)
import net.minecraft.world.item.DyeColor;                                  // Minecraft's 16 dye colors (for colored wires later)
import net.neoforged.neoforge.client.network.ClientPacketDistributor;      // sends packets from this game to the server

import java.util.ArrayList;  // a resizable list we can add rows to
import java.util.List;       // the general "list" type

/**
 * The breadboard window: draws the board, its holes, a highlight under the mouse,
 * draws the board's wires, and lets the player pick two holes to make a new wire.
 * "extends AbstractContainerScreen<BreadboardMenu>" means: this is a menu window,
 * and the menu it belongs to is a BreadboardMenu.
 */
public class BreadboardScreen extends AbstractContainerScreen<BreadboardMenu> {

    // =====================================================================
    // LAYOUT CONSTANTS (all in pixels)
    // "private static final" = only this class uses it, one shared copy, never changes.
    // Change a number here and the whole layout adjusts automatically.
    // =====================================================================
    private static final int CELL = 8;      // each hole gets an 8x8 square of space
    private static final int MARGIN = 8;    // empty space at the window's edges
    private static final int GRID_TOP = 20; // the first row starts 20px down, leaving room for the title
    private static final int RAIL_GAP = 6;  // extra space between power rails and terminal rows
    private static final int CHANNEL = 8;   // height of the center channel that splits e from f

    // =====================================================================
    // COLORS, written as 0xAARRGGBB
    //   AA = transparency (FF = fully visible, 00 = invisible)
    //   RR, GG, BB = red, green, blue, each from 00 to FF
    // =====================================================================
    private static final int BORDER = 0xFF373737;        // dark gray window outline
    private static final int PANEL = 0xFFC6C6C6;         // light gray window background (vanilla's GUI color)
    private static final int BOARD = 0xFFECEAE2;         // off-white breadboard plastic
    private static final int CHANNEL_COLOR = 0xFFC4C2B8; // slightly darker strip for the channel
    private static final int HOLE = 0xFF46464A;          // dark gray holes
    private static final int RED = 0xFFCD3232;           // positive (+) rail stripe
    private static final int BLUE = 0xFF325AC8;          // negative (-) rail stripe
    private static final int HIGHLIGHT = 0xFFFFD84A;     // yellow ring around the hovered hole
    private static final int SELECTED = 0xFF55CC55;      // green ring around a wire's chosen start hole
    private static final int TEXT = 0xFF404040;          // dark gray text, like vanilla titles
    private static final int COPPER = 0xFFC46E40;        // bare copper wire
    private static final int PREVIEW = 0x99C46E40;       // see-through copper: the wire you're about to place (99 = partly transparent)

    // =====================================================================
    // SMALL DATA TYPES
    // A "record" is a quick way to make a tiny class that just holds values.
    // Java writes the getters for you: row.y(), row.name(), etc.
    // It also writes equals(), so two records with the same values count as equal.
    // =====================================================================

    /**
     * One row of holes.
     * y            = how far down the window it sits (pixels, relative to the window's top)
     * name         = what players call it: "+", "−", or a letter like "c"
     * positiveRail = true if this is a red + rail row
     * negativeRail = true if this is a blue − rail row
     * channelAbove = true if the center channel sits right above this row (row f, and p on large boards)
     */
    private record Row(int y, String name, boolean positiveRail, boolean negativeRail, boolean channelAbove) {}

    /**
     * One spot on the board.
     * column = which column (0 = leftmost)
     * row    = which row, as a position in the "rows" list (0 = top)
     */
    private record Hole(int column, int row) {}

    // =====================================================================
    // FIELDS: the data THIS particular window holds
    // =====================================================================
    private final int columns;     // how many columns wide this board is (15 or 30)
    private final List<Row> rows;  // every row, top to bottom, built once when the window opens

    // The first hole clicked when making a wire.
    // null = not making a wire right now.
    // NOT final, because it changes as the player clicks.
    private Hole wireStart = null;

    /**
     * Constructor: runs once when the window opens.
     * The game passes in the menu, the player's inventory, and the window title.
     */
    public BreadboardScreen(BreadboardMenu menu, Inventory playerInventory, Component title) {
        // "super" builds the vanilla part of the window. It must come first.
        // The last two values are the window's width and height in pixels.
        super(menu, playerInventory, title,
                // Width: wide enough for every column plus margins, but never under 176 (vanilla's normal width)
                Math.max(176, menu.getColumns() * CELL + MARGIN * 2),
                // Height: worked out from the row layout (see windowHeight below)
                windowHeight(menu.getStrips()));

        // Now that super is done, we can save our own data
        this.columns = menu.getColumns();          // copy the column count from the menu
        this.rows = buildRows(menu.getStrips());   // build the list of rows for this many strips
    }

    // =====================================================================
    // BUILDING THE LAYOUT
    // =====================================================================

    /**
     * Walks down the board from top to bottom and records every row.
     * Layout: rails, strip, rails, strip, ..., rails.
     * There is always ONE MORE pair of rails than strips.
     *
     * "static" = doesn't depend on any one window's data, so it can be
     * used inside the super(...) call above (via windowHeight).
     */
    private static List<Row> buildRows(int strips) {
        List<Row> rows = new ArrayList<>(); // start with an empty list
        int y = GRID_TOP;                   // the current height we're placing rows at; moves down as we go
        int letter = 0;                     // which letter we're on: 0 = 'a', 1 = 'b', ... keeps counting across strips

        // s goes 0, 1, ... up to strips (inclusive), so this runs strips + 1 times.
        // Each pass adds a pair of rails, then (except on the last pass) a strip.
        for (int s = 0; s <= strips; s++) {

            // ---- A pair of power rails ----
            rows.add(new Row(y, "+", true, false, false)); // red + rail
            y += CELL;                                      // move down one row
            rows.add(new Row(y, "−", false, true, false)); // blue − rail
            y += CELL;                                      // move down one row

            // If this was the last pair of rails, the board is finished.
            // "break" jumps out of the loop immediately.
            if (s == strips) break;

            y += RAIL_GAP; // leave a gap between the rails and the terminal rows

            // ---- A strip: 10 terminal rows with the channel after the 5th ----
            for (int i = 0; i < 10; i++) {
                boolean channelAbove = (i == 5); // the 6th row (f, or p) sits just below the channel
                if (channelAbove) y += CHANNEL;  // leave room for the channel before placing it

                // (char) ('a' + letter) turns a number into a letter: 0 -> 'a', 1 -> 'b', 10 -> 'k'
                // String.valueOf turns that single character into text
                rows.add(new Row(y, String.valueOf((char) ('a' + letter)), false, false, channelAbove));

                letter++;  // next row gets the next letter
                y += CELL; // move down one row
            }

            y += RAIL_GAP; // gap before the next pair of rails
        }
        return rows; // hand back the finished list
    }

    /**
     * How tall the window needs to be: wherever the last row ends,
     * plus room for that row itself, a margin, and the board's edge.
     */
    private static int windowHeight(int strips) {
        List<Row> rows = buildRows(strips);       // build the layout just to measure it
        Row lastRow = rows.get(rows.size() - 1);  // the bottom row (lists start at 0, so the last is size - 1)
        return lastRow.y() + CELL + MARGIN + 4;   // its top, plus its height, plus padding
    }

    // =====================================================================
    // GRID MATH
    // Drawing AND the mouse both use these, so they always agree.
    // =====================================================================

    /**
     * Where the grid's left edge is, measured from the window's left edge.
     * Centers the grid: (window width - grid width) split equally on both sides.
     */
    private int gridLeft() {
        return (imageWidth - columns * CELL) / 2;
    }

    /**
     * Turns a mouse position into a hole, or null if the mouse isn't over one.
     * This is the drawing math run backwards.
     * mouseX and mouseY are SCREEN positions, so we subtract the window's position first.
     */
    private Hole holeAt(double mouseX, double mouseY) {
        // ---- Column: columns are evenly spaced, so just divide ----
        double x = mouseX - leftPos - gridLeft();       // mouse position measured from the grid's left edge
        if (x < 0 || x >= columns * CELL) return null;  // left of the grid or right of it -> no hole
        int column = (int) (x / CELL);                  // e.g. x = 20 -> 20 / 8 = 2.5 -> (int) chops to column 2

        // ---- Row: rows have gaps, so check each one ----
        double y = mouseY - topPos;                // mouse position measured from the window's top
        for (int i = 0; i < rows.size(); i++) {    // try every row
            int top = rows.get(i).y();             // this row's top edge
            if (y >= top && y < top + CELL) {      // is the mouse inside this row's 8px band?
                return new Hole(column, i);        // yes -> that's the hole
            }
        }
        return null; // the mouse is in a gap or the channel -> no hole
    }

    /**
     * A hole's real breadboard name, like "c12".
     * Row name from the list, plus the column number.
     * Columns count from 0 in code, but people count from 1, so add 1.
     */
    private String holeName(Hole hole) {
        return rows.get(hole.row()).name() + (hole.column() + 1);
    }

    /** The CENTER of a hole, as a screen x position. +4 = half a cell. */
    private int centerX(int column) {
        return leftPos + gridLeft() + column * CELL + 4;
    }

    /** The CENTER of a hole, as a screen y position. */
    private int centerY(int row) {
        return topPos + rows.get(row).y() + 4;
    }

    // =====================================================================
    // WIRES
    // =====================================================================

    /**
     * The wires on this board, read from the CLIENT's copy of the block entity.
     * The server keeps that copy up to date (see BreadboardBlockEntity.addWire).
     * Returns an empty list if the board can't be found (for example, it was just broken).
     */
    private List<Wire> boardWires() {
        if (this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(menu.getPos()) instanceof BreadboardBlockEntity board) {
            return board.getWires();
        }
        return List.of(); // an empty list
    }

    /**
     * What color to draw a wire.
     * Bare wires are copper. Colored (insulated) wires use the dye's color.
     * "| 0xFF000000" forces the transparency to FF (fully visible).
     */
    private static int wireColor(Wire wire) {
        if (wire.isBare()) return COPPER;
        return DyeColor.byId(wire.color()).getTextureDiffuseColor() | 0xFF000000;
    }

    /**
     * Draws a straight line, 2 pixels thick, from (x0, y0) to (x1, y1).
     * Minecraft's GUI has no "draw a line" tool, so we make one:
     * take enough small steps along the line, and draw a 2x2 square at each step.
     */
    private static void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;                                      // how far to go sideways
        int dy = y1 - y0;                                      // how far to go up/down
        int steps = Math.max(Math.abs(dx), Math.abs(dy));      // one step per pixel along the longer direction
        if (steps == 0) steps = 1;                             // avoid dividing by zero for a zero-length line
        for (int i = 0; i <= steps; i++) {
            int px = x0 + dx * i / steps;                      // i/steps of the way along, sideways
            int py = y0 + dy * i / steps;                      // i/steps of the way along, up/down
            g.fill(px - 1, py - 1, px + 1, py + 1, color);     // a 2x2 dot
        }
    }

    // =====================================================================
    // MOUSE INPUT
    // =====================================================================

    /**
     * Runs whenever the player clicks inside this window.
     * Return true  = "I handled this click, don't do anything else with it."
     * Return super = "not mine, let vanilla deal with it" (keeps normal window behavior working).
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // event.button(): 0 = left mouse button, 1 = right mouse button

        // ---- Right-click anywhere while making a wire: cancel it ----
        if (event.button() == 1 && wireStart != null) {
            wireStart = null; // forget the start hole
            return true;
        }

        // ---- Left-click on a hole ----
        Hole clicked = holeAt(event.x(), event.y()); // which hole was clicked? (null if none)
        if (event.button() == 0 && clicked != null) {

            if (wireStart == null) {
                // First click: this hole becomes the wire's start
                wireStart = clicked;

            } else if (wireStart.equals(clicked)) {
                // Clicked the start hole again: cancel
                wireStart = null;

            } else {
                // Second click on a different hole: ask the SERVER to add this wire.
                // The server checks it, takes a copper wire, saves it, and sends the board back to us.
                // We always ask for BARE; the server decides colors.
                ClientPacketDistributor.sendToServer(new AddWirePayload(menu.getPos(),
                        new Wire(wireStart.column(), wireStart.row(), clicked.column(), clicked.row(), Wire.BARE)));
                wireStart = null; // ready to start the next wire
            }
            return true;
        }

        // ---- Any other click: not ours ----
        return super.mouseClicked(event, doubleClick);
    }

    // =====================================================================
    // DRAWING
    // Each frame, the game calls these methods to paint the window.
    // Later fills paint ON TOP of earlier ones, like layers of paint.
    // =====================================================================

    /**
     * Draws everything behind the text: window, board, rails, highlights, holes, wires.
     * "public" because vanilla's version is public, and an override can't be more private.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick); // let vanilla draw its part first

        // Short names for positions we use a lot. These are SCREEN positions.
        int x = leftPos, y = topPos;                 // the window's top-left corner on screen
        int left = x + gridLeft();                   // the grid's left edge on screen
        int right = left + columns * CELL;           // the grid's right edge on screen
        int boardTop = y + rows.get(0).y() - 4;      // board starts 4px above the first row
        int boardBottom = y + rows.get(rows.size() - 1).y() + CELL + 4; // and ends 4px below the last row

        // ---- Layer 1: the window ----
        // fill(left, top, right, bottom, color) draws a solid rectangle
        g.fill(x, y, x + imageWidth, y + imageHeight, BORDER);                // dark outline (full window)
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL); // gray panel, 2px smaller on every side

        // ---- Layer 2: the white board ----
        g.fill(left - 4, boardTop, right + 4, boardBottom, BOARD); // 4px wider than the grid on each side

        // ---- Layer 3: channels and rail stripes, read from the row list ----
        // "for (Row row : rows)" = "for each row in the list, call it row"
        for (Row row : rows) {
            int ry = y + row.y(); // this row's top edge on screen

            // Channel: a strip in the gap just above this row
            if (row.channelAbove()) g.fill(left - 4, ry - CHANNEL + 2, right + 4, ry - 2, CHANNEL_COLOR);

            // Red line just ABOVE a + rail
            if (row.positiveRail()) g.fill(left, ry - 2, right, ry - 1, RED);

            // Blue line just BELOW a − rail
            if (row.negativeRail()) g.fill(left, ry + CELL + 1, right, ry + CELL + 2, BLUE);
        }

        // ---- Layer 4: hover highlight (yellow) ----
        // Drawn BEFORE the holes: the hole then paints over its center, leaving a ring
        Hole hovered = holeAt(mouseX, mouseY); // which hole is the mouse over? (null if none)
        if (hovered != null) {
            int hx = left + hovered.column() * CELL;         // the hovered cell's left edge
            int hy = y + rows.get(hovered.row()).y();        // the hovered cell's top edge
            g.fill(hx, hy, hx + CELL, hy + CELL, HIGHLIGHT); // fill the whole 8x8 cell yellow
        }

        // ---- Layer 5: wire start (green) ----
        // Drawn AFTER the yellow, so hovering over the start hole keeps it green
        if (wireStart != null) {
            int sx = left + wireStart.column() * CELL;
            int sy = y + rows.get(wireStart.row()).y();
            g.fill(sx, sy, sx + CELL, sy + CELL, SELECTED);
        }

        // ---- Layer 6: the holes ----
        for (Row row : rows) {                          // every row...
            for (int col = 0; col < columns; col++) {   // ...and every column in it
                int hx = left + col * CELL + 2;         // +2 centers a 4px hole in an 8px cell
                int hy = y + row.y() + 2;
                g.fill(hx, hy, hx + 4, hy + 4, HOLE);   // a 4x4 dark square
            }
        }

        // ---- Layer 7: placed wires ----
        // Drawn AFTER the holes, so wires sit on top of the board like real jumper wires
        for (Wire wire : boardWires()) {
            drawLine(g,
                    centerX(wire.startCol()), centerY(wire.startRow()),
                    centerX(wire.endCol()), centerY(wire.endRow()),
                    wireColor(wire));
        }

        // ---- Layer 8: preview of the wire you're about to place ----
        // A see-through line from the start hole to whatever hole you're hovering
        if (wireStart != null && hovered != null && !hovered.equals(wireStart)) {
            drawLine(g,
                    centerX(wireStart.column()), centerY(wireStart.row()),
                    centerX(hovered.column()), centerY(hovered.row()),
                    PREVIEW);
        }
    }

    /**
     * Draws text on top of everything.
     * NOTE: positions here are RELATIVE to the window's top-left corner,
     * unlike extractBackground, which uses screen positions.
     */
    @Override
    public void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // The window title, like "Large Breadboard (2×30)"
        // text(font, words, x, y, color, shadow?) - false = no drop shadow, like vanilla
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        // The hovered hole's name in the top right corner, like "c12"
        Hole hovered = holeAt(mouseX, mouseY);
        if (hovered != null) {
            Component info = Component.literal(holeName(hovered));

            // Right-align it: start at (window width - margin - the text's width)
            g.text(this.font, info, imageWidth - MARGIN - font.width(info), titleLabelY, TEXT, false);
        }
    }
}