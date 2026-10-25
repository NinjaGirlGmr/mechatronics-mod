package com.gamer.mechatronics.item;

import net.minecraft.world.item.Item;

public class SwitchItem extends Item {
    // Safe limits for this switch, like the rating printed on a real one
    public static final double MAX_VOLTAGE = 12.0;
    public static final int MAX_CURRENT_MA = 500;

    public SwitchItem(Properties properties) {
        super(properties);
    }
}