package com.gamer.mechatronics.item;
import net.minecraft.world.item.Item;

public class LedItem extends Item{
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


}
