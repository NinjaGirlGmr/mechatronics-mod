package com.gamer.mechatronics.part;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/**
 * Anything that can go on a breadboard.
 * Implement this on an Item class and the breadboard, sidebar, packets, and tooltips
 * will all handle it automatically. No breadboard code needs to change.
 *
 * "default" methods come with a ready-made answer, so a part only
 * overrides what's different about it.
 */
public interface BoardPart {

    /** How many legs (holes) this part uses. Every part is 2 for now. */
    default int legs() {
        return 2;
    }

    /**
     * Does direction matter? If true, the first hole clicked is the + leg.
     * LEDs are polarized; resistors and switches are not.
     */
    default boolean polarized() {
        return false;
    }

    /**
     * Which sidebar slot this part belongs to.
     * Parts with the same group share one slot (like all 16 LED colors).
     * By default, each item is its own group, named after the item (like "mechatronics:resistor").
     * (Item) this works because parts are always items.
     */
    default Identifier sidebarGroup() {
        return BuiltInRegistries.ITEM.getKey((Item) this);
    }

    /**
     * This part's electrical stats, for its tooltip.
     * Each part adds its own lines by calling lines.accept(...).
     * Parts with nothing to say can skip this.
     */
    default void addStats(Consumer<Component> lines) {
    }
}