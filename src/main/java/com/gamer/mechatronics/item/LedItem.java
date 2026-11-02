package com.gamer.mechatronics.item;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.part.BoardPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/**
 * An LED: lights up when enough current flows the right way through it.
 * Fits on a breadboard (2 legs, polarized: the first click is the + leg).
 */
public class LedItem extends Item implements BoardPart {

    // Volts needed before this LED lights up. Each color is different.
    private final double forwardVoltage;

    // Current that burns it out, in milliamps. Same for all LEDs for now.
    public static final int MAX_CURRENT_MA = 20;

    public LedItem(Properties properties, double forwardVoltage) {
        super(properties);
        this.forwardVoltage = forwardVoltage;
    }

    public double getForwardVoltage() {
        return forwardVoltage;
    }

    // LEDs only work one way: the first click is the + leg (anode, the long leg)
    @Override
    public boolean polarized() {
        return true;
    }

    // All 16 colors share one sidebar slot
    @Override
    public Identifier sidebarGroup() {
        return Identifier.fromNamespaceAndPath(Mechatronics.MODID, "led");
    }

    // This LED's stats, shown in its tooltip
    @Override
    public void addStats(Consumer<Component> lines) {
        lines.accept(Component.literal("Forward voltage: " + forwardVoltage + " V"));
        lines.accept(Component.literal("Max current: " + MAX_CURRENT_MA + " mA"));
    }
}