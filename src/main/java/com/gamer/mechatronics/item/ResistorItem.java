package com.gamer.mechatronics.item;

import com.gamer.mechatronics.part.BoardPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/**
 * A resistor: limits how much current flows. Fits on a breadboard (2 legs, either direction).
 */
public class ResistorItem extends Item implements BoardPart {

    // Resistance in ohms. 1000 = 1 kΩ, a common value for LED circuits.
    // Fixed for now; later the player can pick a value.
    public static final int RESISTANCE_OHMS = 1000;

    // Most power it can handle before burning, in watts. 1/4 W is the standard small resistor.
    public static final double MAX_POWER_WATTS = 0.25;

    public ResistorItem(Properties properties) {
        super(properties);
    }

    // This resistor's stats, shown in its tooltip
    @Override
    public void addStats(Consumer<Component> lines) {
        lines.accept(Component.literal("Resistance: " + formatOhms(RESISTANCE_OHMS)));
        lines.accept(Component.literal("Power rating: " + MAX_POWER_WATTS + " W"));
    }

    // Writes ohms the way engineers do: 220 Ω, 1 kΩ, 4.7 kΩ, 1 MΩ
    private static String formatOhms(int ohms) {
        if (ohms >= 1_000_000) return trim(ohms / 1_000_000.0) + " MΩ";  // mega = million
        if (ohms >= 1_000) return trim(ohms / 1_000.0) + " kΩ";          // kilo = thousand
        return ohms + " Ω";
    }

    // 1.0 -> "1", 4.7 -> "4.7" (drops a pointless ".0")
    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}