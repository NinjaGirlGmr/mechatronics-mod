package com.gamer.mechatronics.item;

import com.gamer.mechatronics.part.BoardPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/**
 * A toggle switch: connects or disconnects its two legs.
 * Fits on a breadboard (2 legs, either direction).
 */
public class SwitchItem extends Item implements BoardPart {

    // Safe limits for this switch, like the rating printed on a real one
    public static final double MAX_VOLTAGE = 12.0;
    public static final int MAX_CURRENT_MA = 500;

    public SwitchItem(Properties properties) {
        super(properties);
    }

    // This switch's stats, shown in its tooltip
    @Override
    public void addStats(Consumer<Component> lines) {
        // SPST = single pole, single throw: one circuit, either connected or not
        lines.accept(Component.literal("Type: SPST toggle"));
        lines.accept(Component.literal("Rated: " + MAX_VOLTAGE + " V, " + MAX_CURRENT_MA + " mA"));
    }
}