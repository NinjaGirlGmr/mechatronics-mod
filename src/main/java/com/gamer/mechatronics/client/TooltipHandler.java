package com.gamer.mechatronics.client;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.item.LedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Adds electrical info to our parts' tooltips. Client only: servers don't draw tooltips.
@EventBusSubscriber(modid = Mechatronics.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // Is the hovered item one of our LEDs? If so, call it "led"
        if (event.getItemStack().getItem() instanceof LedItem led) {
            event.getToolTip().add(Component.literal("Forward voltage: " + led.getForwardVoltage() + " V")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Max current: " + LedItem.MAX_CURRENT_MA + " mA")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}