package com.gamer.mechatronics.client;

// ---- Imports: classes from other packages this file uses ----
import com.gamer.mechatronics.Mechatronics;                                // our mod (for COPPER_WIRE and MODID)
import com.gamer.mechatronics.block.BreadboardBlockEntity;                 // the board's data (its wires)
import com.gamer.mechatronics.block.Wire;                                  // one wire: two holes + a color
import com.gamer.mechatronics.menu.BreadboardMenu;                         // our menu (holds columns, strips, pos)
import com.gamer.mechatronics.network.AddWirePayload;                      // the packet that asks the server to add a wire
import com.gamer.mechatronics.network.RemoveWirePayload;                   // the packet that asks the server to remove a wire
import com.gamer.mechatronics.part.BoardPart;                              // anything that can go on a breadboard
import net.minecraft.client.gui.GuiGraphicsExtractor;                      // the "paintbrush" for drawing on screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen; // vanilla's base class for menu windows
import net.minecraft.client.input.MouseButtonEvent;                        // a mouse click: where, and which button
import net.minecraft.network.chat.Component;                               // text that can be shown in game
import net.minecraft.resources.Identifier;                                 // a namespaced name like "mechatronics:led"
import net.minecraft.world.entity.player.Inventory;                        // the player's inventory
import net.minecraft.world.item.DyeColor;                                  // Minecraft's 16 dye colors (for colored wires later)
import net.minecraft.world.item.ItemStack;                                 // a stack of items (what goes in a slot)
import net.neoforged.neoforge.client.network.ClientPacketDistributor;      // sends packets from this game to the server

import java.util.ArrayList;      // a resizable list
import java.util.LinkedHashMap;  // a map that remembers the order things were added
import java.util.List;           // the general "list" type
import java.util.Map;            // the general "map" type (key -> value)

/**
 * The breadboard window.
 * Left side: the board, its holes and wires. Pick two holes to place a wire; right-click a wire to remove it.
 * Right side: the parts sidebar. Shows the parts you own, with counts. Click one to select it.
 * "extends AbstractContainerScreen<BreadboardMenu>" means: this is a menu window,
 * and the menu it belongs to is a BreadboardMenu.
 */
public class BreadboardScreen extends AbstractContainerScreen<BreadboardMenu> {

    // =====================================================================
    // LAYOUT CONSTANTS (all in pixels)
    // "private static final" = only this class uses it, one shared copy, never changes.
    // Change a number here and the whole layout adjusts automatically.
    // =====================================================================

    // ---- Board ----
    private static final int CELL = 8;      // each hole gets an 8x8 square of space
    private static final int MARGIN = 8;    // empty space at the window's edges
    private static final int GRID_TOP = 20; // the first row starts 20px down, leaving room for the title
    private static final int RAIL_GAP = 6;  // extra space between power rails and terminal rows
    private static final int CHANNEL = 8;   // height of the center channel that splits e from f

    // ---- Sidebar ----
    private static final int SLOT = 18;                                 // one slot, the same size as vanilla's (16px item + 1px border each side)
    private static final int SLOT_GAP = 2;                              // space between the two columns
    private static final int SIDE_PAD = 6;                              // padding inside the sidebar
    private static final int SIDEBAR_W = SIDE_PAD * 2 + SLOT * 2 + SLOT_GAP; // total sidebar width: two columns of slots
    private static final int COLOR_LABEL_Y = 8;                         // "Color" label
    private static final int COLOR_TOP = 18;                            // the color swatch slot
    private static final int PARTS_LABEL_Y = 42;                        // "Parts" label
    private static final int PARTS_TOP = 52;                            // the first row of part slots

    // =====================================================================
    // COLORS, written as 0xAARRGGBB
    //   AA = transparency (FF = fully visible, 00 = invisible)
    //   RR, GG, BB = red, green, blue, each from 00 to FF
    // Dark theme: dark grays for the window, a softer off-white for the board.
    // =====================================================================

    // ---- Window ----
    private static final int BORDER = 0xFF141416;        // near-black window outline
    private static final int PANEL = 0xFF2B2B30;         // dark gray window background
    private static final int SIDEBAR_BG = 0xFF222226;    // the sidebar, a little darker than the window
    private static final int TEXT = 0xFFE0E0E0;          // light text (shows up on dark backgrounds)
    private static final int TEXT_DIM = 0xFF9A9AA0;      // quieter text for labels

    // ---- Board ----
    private static final int BOARD = 0xFFD6D2C6;         // softer off-white: easier on the eyes than pure white
    private static final int CHANNEL_COLOR = 0xFFB9B5A9; // slightly darker strip for the channel
    private static final int HOLE = 0xFF46464A;          // dark gray holes
    private static final int RED = 0xFFCD3232;           // positive (+) rail stripe
    private static final int BLUE = 0xFF325AC8;          // negative (-) rail stripe
    private static final int HIGHLIGHT = 0xFFFFD84A;     // yellow ring around the hovered hole
    private static final int SELECTED = 0xFF55CC55;      // green ring around a wire's chosen start hole

    // ---- Wires ----
    private static final int COPPER = 0xFFC46E40;        // bare copper wire
    private static final int PREVIEW = 0x99C46E40;       // see-through copper: the wire you're about to place (99 = partly transparent)
    private static final int WIRE_HOVER = 0xFFE04040;    // red: the wire under the mouse, which a right-click would remove
    private static final double WIRE_GRAB = 3.0;         // how close (in pixels) the mouse must be to a wire to "touch" it

    // ---- Slots (like the hotbar, but darker) ----
    private static final int SLOT_SHADOW = 0xFF0E0E10;   // top and left edge: looks sunken in
    private static final int SLOT_SHINE = 0xFF3C3C42;    // bottom and right edge: catches the light
    private static final int SLOT_INNER = 0xFF1A1A1D;    // the inside of the slot
    private static final int SLOT_SELECTED = 0xFFE8E8E8; // white outline around the selected slot
    private static final int SLOT_EMPTY = 0xB0222226;    // see-through dark layer that grays out parts you have none of

    // The sidebar group for copper wire. Wire isn't a BoardPart (it has its own system), so it gets a fixed name.
    private static final Identifier WIRE_GROUP = Identifier.fromNamespaceAndPath(Mechatronics.MODID, "copper_wire");

    // =====================================================================
    // SMALL DATA TYPES
    // A "record" is a quick way to make a tiny class that just holds values.
    // Java writes the getters for you, and equals() so matching records count as equal.
    // =====================================================================

    /**
     * One row of holes.
     * y            = how far down the window it sits (pixels, relative to the window's top)
     * name         = what players call it: "+", "−", or a letter like "c"
     * positiveRail = true if this is a red + rail row
     * negativeRail = true if this is a blue − rail row
     * channelAbove = true if the center channel sits right above this row
     */
    private record Row(int y, String name, boolean positiveRail, boolean negativeRail, boolean channelAbove) {}

    /** One spot on the board. column = 0 is leftmost; row = position in the "rows" list, 0 = top. */
    private record Hole(int column, int row) {}

    /**
     * One slot in the sidebar.
     * group    = which sidebar group this is (like "mechatronics:led")
     * variants = one stack of each different item you own in this group (like every LED color you have)
     * count    = how many you have in total, across every variant
     */
    private record SidebarEntry(Identifier group, List<ItemStack> variants, int count) {

        /**
         * The icon to show. If there are several variants, it cycles through them once a second,
         * like the recipe book flipping through ingredient options.
         * currentTimeMillis / 1000 = seconds; % variants.size() wraps it around to a valid index.
         */
        ItemStack icon() {
            int index = (int) ((System.currentTimeMillis() / 1000) % variants.size());
            return variants.get(index);
        }
    }

    // =====================================================================
    // FIELDS: the data THIS particular window holds
    // =====================================================================
    private final int columns;     // how many columns wide this board is (15 or 30)
    private final List<Row> rows;  // every row, top to bottom, built once when the window opens

    // The first hole clicked when making a wire (null = not making a wire right now)
    private Hole wireStart = null;

    // Which sidebar group is selected. Starts on copper wire.
    private Identifier selectedGroup = WIRE_GROUP;

    /**
     * Constructor: runs once when the window opens.
     */
    public BreadboardScreen(BreadboardMenu menu, Inventory playerInventory, Component title) {
        // "super" builds the vanilla part of the window. It must come first.
        // Width = the board area plus the sidebar. Height comes from the row layout.
        super(menu, playerInventory, title,
                boardAreaWidth(menu.getColumns()) + SIDEBAR_W,
                windowHeight(menu.getStrips()));

        this.columns = menu.getColumns();
        this.rows = buildRows(menu.getStrips());
    }

    // =====================================================================
    // BUILDING THE LAYOUT
    // =====================================================================

    /**
     * How wide the board's part of the window is (everything left of the sidebar).
     * Wide enough for every column plus margins, but never under 176 (vanilla's normal width).
     */
    private static int boardAreaWidth(int columns) {
        return Math.max(176, columns * CELL + MARGIN * 2);
    }

    /**
     * Walks down the board from top to bottom and records every row.
     * Layout: rails, strip, rails, strip, ..., rails. There is always ONE MORE pair of rails than strips.
     */
    private static List<Row> buildRows(int strips) {
        List<Row> rows = new ArrayList<>();
        int y = GRID_TOP;   // the current height we're placing rows at; moves down as we go
        int letter = 0;     // 0 = 'a', 1 = 'b', ... keeps counting across strips

        for (int s = 0; s <= strips; s++) {
            // ---- A pair of power rails ----
            rows.add(new Row(y, "+", true, false, false));
            y += CELL;
            rows.add(new Row(y, "−", false, true, false));
            y += CELL;

            if (s == strips) break; // the last rails close off the bottom
            y += RAIL_GAP;

            // ---- A strip: 10 terminal rows with the channel after the 5th ----
            for (int i = 0; i < 10; i++) {
                boolean channelAbove = (i == 5);
                if (channelAbove) y += CHANNEL;
                rows.add(new Row(y, String.valueOf((char) ('a' + letter)), false, false, channelAbove));
                letter++;
                y += CELL;
            }
            y += RAIL_GAP;
        }
        return rows;
    }

    /** How tall the window needs to be: wherever the last row ends, plus padding. */
    private static int windowHeight(int strips) {
        List<Row> rows = buildRows(strips);
        Row lastRow = rows.get(rows.size() - 1);
        return lastRow.y() + CELL + MARGIN + 4;
    }

    // =====================================================================
    // GRID MATH
    // Drawing AND the mouse both use these, so they always agree.
    // =====================================================================

    /** Where the sidebar starts, measured from the window's left edge. */
    private int sidebarLeft() {
        return imageWidth - SIDEBAR_W;
    }

    /** Where the grid's left edge is, measured from the window's left edge. Centered in the board area. */
    private int gridLeft() {
        return (sidebarLeft() - columns * CELL) / 2;
    }

    /** Turns a mouse position into a hole, or null if the mouse isn't over one. */
    private Hole holeAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos - gridLeft();
        if (x < 0 || x >= columns * CELL) return null;
        int column = (int) (x / CELL);

        double y = mouseY - topPos;
        for (int i = 0; i < rows.size(); i++) {
            int top = rows.get(i).y();
            if (y >= top && y < top + CELL) return new Hole(column, i);
        }
        return null; // in a gap or the channel
    }

    /** A hole's real breadboard name, like "c12". Columns count from 0 in code, so add 1. */
    private String holeName(Hole hole) {
        return rows.get(hole.row()).name() + (hole.column() + 1);
    }

    /** The CENTER of a hole, as a screen x position. */
    private int centerX(int column) {
        return leftPos + gridLeft() + column * CELL + 4;
    }

    /** The CENTER of a hole, as a screen y position. */
    private int centerY(int row) {
        return topPos + rows.get(row).y() + 4;
    }

    // =====================================================================
    // SIDEBAR
    // =====================================================================

    /**
     * Looks through the player's inventory and builds the sidebar's slots.
     * Copper wire always comes first (grayed out if you have none).
     * Then one slot per BoardPart group you own, in the order they appear in your inventory.
     * Rebuilt every frame, so it updates the moment your inventory changes.
     */
    private List<SidebarEntry> buildSidebar() {
        Inventory inventory = this.minecraft.player.getInventory();
        int wireCount = 0;

        // For each group: the different items you own in it, and how many in total.
        // LinkedHashMap remembers the order groups were first found.
        Map<Identifier, List<ItemStack>> variants = new LinkedHashMap<>();
        Map<Identifier, Integer> counts = new LinkedHashMap<>();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue; // skip empty slots

            // Copper wire: just count it
            if (stack.is(Mechatronics.COPPER_WIRE.get())) {
                wireCount += stack.getCount();
                continue;
            }

            // Any breadboard part: add it to its group
            if (stack.getItem() instanceof BoardPart part) {
                Identifier group = part.sidebarGroup();

                // Get this group's list of variants, making a new empty list if it's the first one
                List<ItemStack> groupVariants = variants.computeIfAbsent(group, key -> new ArrayList<>());

                // Add this item as a variant, unless we already have it (two stacks of red LEDs = one variant)
                boolean alreadyListed = groupVariants.stream().anyMatch(s -> s.is(stack.getItem()));
                if (!alreadyListed) groupVariants.add(new ItemStack(stack.getItem()));

                // Add to the group's total. merge = "add to the existing number, or start at this one"
                counts.merge(group, stack.getCount(), Integer::sum);
            }
        }

        // Put it all together: wire first, then every part group
        List<SidebarEntry> entries = new ArrayList<>();
        entries.add(new SidebarEntry(WIRE_GROUP, List.of(new ItemStack(Mechatronics.COPPER_WIRE.get())), wireCount));
        for (Map.Entry<Identifier, List<ItemStack>> group : variants.entrySet()) {
            entries.add(new SidebarEntry(group.getKey(), group.getValue(), counts.get(group.getKey())));
        }
        return entries;
    }

    /** Where slot number "index" sits, measured from the window's top-left. Two columns, filling left to right. */
    private int slotX(int index) {
        return sidebarLeft() + SIDE_PAD + (index % 2) * (SLOT + SLOT_GAP); // % 2 = which column (0 or 1)
    }

    private int slotY(int index) {
        return PARTS_TOP + (index / 2) * SLOT;                             // / 2 = which row
    }

    /** Which sidebar slot the mouse is over, or -1 if none. */
    private int slotAt(double mouseX, double mouseY, List<SidebarEntry> entries) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        for (int i = 0; i < entries.size(); i++) {
            if (x >= slotX(i) && x < slotX(i) + SLOT && y >= slotY(i) && y < slotY(i) + SLOT) return i;
        }
        return -1;
    }

    /**
     * Draws one empty slot, styled like the hotbar but darker.
     * x, y are SCREEN positions. A dark top-left edge and a lighter bottom-right edge make it look sunken.
     */
    private static void drawSlot(GuiGraphicsExtractor g, int x, int y, boolean selected) {
        g.fill(x, y, x + SLOT, y + SLOT, SLOT_SHADOW);                    // whole square: shadow color
        g.fill(x + 1, y + 1, x + SLOT, y + SLOT, SLOT_SHINE);             // shifted 1px: shine shows on bottom-right
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_INNER);     // the inside, leaving 1px edges
        if (selected) g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, SLOT_SELECTED); // white frame around it
    }

    // =====================================================================
    // WIRES
    // =====================================================================

    /** The wires on this board, from the CLIENT's copy of the block entity (kept up to date by the server). */
    private List<Wire> boardWires() {
        if (this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(menu.getPos()) instanceof BreadboardBlockEntity board) {
            return board.getWires();
        }
        return List.of();
    }

    /** What color to draw a wire. Bare = copper; colored (insulated) = its dye color. */
    private static int wireColor(Wire wire) {
        if (wire.isBare()) return COPPER;
        return DyeColor.byId(wire.color()).getTextureDiffuseColor() | 0xFF000000;
    }

    /**
     * How far a point (px, py) is from the line segment (x0, y0) -> (x1, y1).
     * Finds the closest spot ON the wire (t from 0 = start to 1 = end), then measures to it with Pythagoras.
     */
    private static double distanceToSegment(double px, double py, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) return Math.hypot(px - x0, py - y0);

        double t = ((px - x0) * dx + (py - y0) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));

        double closestX = x0 + t * dx;
        double closestY = y0 + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    /** Which wire the mouse is touching, or null. Checks the last (top-most drawn) wire first. */
    private Wire wireAt(double mouseX, double mouseY) {
        List<Wire> wires = boardWires();
        for (int i = wires.size() - 1; i >= 0; i--) {
            Wire wire = wires.get(i);
            double distance = distanceToSegment(mouseX, mouseY,
                    centerX(wire.startCol()), centerY(wire.startRow()),
                    centerX(wire.endCol()), centerY(wire.endRow()));
            if (distance <= WIRE_GRAB) return wire;
        }
        return null;
    }

    /** Draws a 2px-thick line by placing a small square at every step along it. */
    private static void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) steps = 1;
        for (int i = 0; i <= steps; i++) {
            int px = x0 + dx * i / steps;
            int py = y0 + dy * i / steps;
            g.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }

    // =====================================================================
    // MOUSE INPUT
    // =====================================================================

    /**
     * Runs whenever the player clicks inside this window.
     * Return true = "I handled this click." Return super = "not mine, let vanilla deal with it."
     * event.button(): 0 = left, 1 = right.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        // ---- Left-click on a sidebar slot: select it ----
        if (event.button() == 0) {
            List<SidebarEntry> entries = buildSidebar();
            int slot = slotAt(event.x(), event.y(), entries);
            if (slot != -1) {
                selectedGroup = entries.get(slot).group();
                wireStart = null; // switching parts cancels a wire in progress
                return true;
            }
        }

        // ---- Right-click ----
        if (event.button() == 1) {
            // While making a wire: cancel it
            if (wireStart != null) {
                wireStart = null;
                return true;
            }
            // Otherwise: if the mouse is on a wire, ask the server to remove it
            Wire touched = wireAt(event.x(), event.y());
            if (touched != null) {
                ClientPacketDistributor.sendToServer(new RemoveWirePayload(menu.getPos(), touched));
                return true;
            }
        }

        // ---- Left-click on a hole ----
        Hole clicked = holeAt(event.x(), event.y());
        if (event.button() == 0 && clicked != null) {

            // Only wire can be placed so far. Placing parts comes in piece D.
            if (!selectedGroup.equals(WIRE_GROUP)) return true;

            if (wireStart == null) {
                wireStart = clicked;                      // first click: the start
            } else if (wireStart.equals(clicked)) {
                wireStart = null;                         // clicked the start again: cancel
            } else {
                // Second click: ask the SERVER to add this wire
                ClientPacketDistributor.sendToServer(new AddWirePayload(menu.getPos(),
                        new Wire(wireStart.column(), wireStart.row(), clicked.column(), clicked.row(), Wire.BARE)));
                wireStart = null;
            }
            return true;
        }

        // ---- Any other click: not ours ----
        return super.mouseClicked(event, doubleClick);
    }

    // =====================================================================
    // DRAWING
    // Later fills paint ON TOP of earlier ones, like layers of paint.
    // =====================================================================

    /**
     * Draws everything behind the text.
     * "public" because vanilla's version is public, and an override can't be more private.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);

        // SCREEN positions we use a lot
        int x = leftPos, y = topPos;                 // the window's top-left corner
        int left = x + gridLeft();                   // the grid's left edge
        int right = left + columns * CELL;           // the grid's right edge
        int boardTop = y + rows.get(0).y() - 4;
        int boardBottom = y + rows.get(rows.size() - 1).y() + CELL + 4;
        int sideX = x + sidebarLeft();               // the sidebar's left edge

        // ---- Layer 1: the window ----
        g.fill(x, y, x + imageWidth, y + imageHeight, BORDER);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL);

        // ---- Layer 2: the board ----
        g.fill(left - 4, boardTop, right + 4, boardBottom, BOARD);

        // ---- Layer 3: channels and rail stripes ----
        for (Row row : rows) {
            int ry = y + row.y();
            if (row.channelAbove()) g.fill(left - 4, ry - CHANNEL + 2, right + 4, ry - 2, CHANNEL_COLOR);
            if (row.positiveRail()) g.fill(left, ry - 2, right, ry - 1, RED);
            if (row.negativeRail()) g.fill(left, ry + CELL + 1, right, ry + CELL + 2, BLUE);
        }

        // ---- Layer 4: hover highlight (yellow, under the holes so it shows as a ring) ----
        Hole hovered = holeAt(mouseX, mouseY);
        if (hovered != null) {
            int hx = left + hovered.column() * CELL;
            int hy = y + rows.get(hovered.row()).y();
            g.fill(hx, hy, hx + CELL, hy + CELL, HIGHLIGHT);
        }

        // ---- Layer 5: wire start (green) ----
        if (wireStart != null) {
            int sx = left + wireStart.column() * CELL;
            int sy = y + rows.get(wireStart.row()).y();
            g.fill(sx, sy, sx + CELL, sy + CELL, SELECTED);
        }

        // ---- Layer 6: the holes ----
        for (Row row : rows) {
            for (int col = 0; col < columns; col++) {
                int hx = left + col * CELL + 2;
                int hy = y + row.y() + 2;
                g.fill(hx, hy, hx + 4, hy + 4, HOLE);
            }
        }

        // ---- Layer 7: placed wires (the one under the mouse turns red, when not placing) ----
        Wire touched = (wireStart == null) ? wireAt(mouseX, mouseY) : null;
        for (Wire wire : boardWires()) {
            int color = wire.equals(touched) ? WIRE_HOVER : wireColor(wire);
            drawLine(g, centerX(wire.startCol()), centerY(wire.startRow()),
                    centerX(wire.endCol()), centerY(wire.endRow()), color);
        }

        // ---- Layer 8: preview of the wire you're about to place ----
        if (wireStart != null && hovered != null && !hovered.equals(wireStart)) {
            drawLine(g, centerX(wireStart.column()), centerY(wireStart.row()),
                    centerX(hovered.column()), centerY(hovered.row()), PREVIEW);
        }

        // ---- Layer 9: the sidebar ----
        // Background, with a dark divider line between it and the board
        g.fill(sideX, y + 2, x + imageWidth - 2, y + imageHeight - 2, SIDEBAR_BG);
        g.fill(sideX, y + 2, sideX + 1, y + imageHeight - 2, BORDER);

        // Color section: just bare copper for now. The palette fills this in later.
        int colorX = sideX + SIDE_PAD;
        int colorY = y + COLOR_TOP;
        drawSlot(g, colorX, colorY, true);
        g.fill(colorX + 3, colorY + 3, colorX + SLOT - 3, colorY + SLOT - 3, COPPER); // a copper swatch

        // Part slots
        List<SidebarEntry> entries = buildSidebar();
        for (int i = 0; i < entries.size(); i++) {
            SidebarEntry entry = entries.get(i);
            int sx = x + slotX(i);
            int sy = y + slotY(i);

            drawSlot(g, sx, sy, entry.group().equals(selectedGroup));
            g.item(entry.icon(), sx + 1, sy + 1);  // the item's icon, 16x16, inside the slot's 1px border

            if (entry.count() == 0) {
                // None left: gray it out
                g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, SLOT_EMPTY);
            } else {
                // How many you have, bottom-right like vanilla. true = drop shadow, so it's readable on the icon
                String count = String.valueOf(entry.count());
                g.text(this.font, count, sx + SLOT - 1 - font.width(count), sy + SLOT - 8, 0xFFFFFFFF, true);
            }
        }
    }

    /**
     * Draws text on top of everything.
     * Positions here are RELATIVE to the window's top-left corner.
     */
    @Override
    public void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Window title, like "Large Breadboard (2×30)"
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        // Hovered hole's name, at the right edge of the board area
        Hole hovered = holeAt(mouseX, mouseY);
        if (hovered != null) {
            Component info = Component.literal(holeName(hovered));
            g.text(this.font, info, sidebarLeft() - MARGIN - font.width(info), titleLabelY, TEXT, false);
        }

        // Sidebar section labels
        g.text(this.font, "Color", sidebarLeft() + SIDE_PAD, COLOR_LABEL_Y, TEXT_DIM, false);
        g.text(this.font, "Parts", sidebarLeft() + SIDE_PAD, PARTS_LABEL_Y, TEXT_DIM, false);
    }
}