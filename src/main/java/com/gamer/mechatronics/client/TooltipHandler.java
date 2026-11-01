package com.gamer.mechatronics.client;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.BreadboardBlock;
import com.gamer.mechatronics.item.LedItem;
import com.gamer.mechatronics.item.SwitchItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Adds electrical info to our parts' tooltips. Client only: servers don't draw tooltips.
@EventBusSubscriber(modid = Mechatronics.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {

        // ---- LEDs ----
        if (event.getItemStack().getItem() instanceof LedItem led) {
            event.getToolTip().add(Component.literal("Forward voltage: " + led.getForwardVoltage() + " V")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Max current: " + LedItem.MAX_CURRENT_MA + " mA")
                    .withStyle(ChatFormatting.GRAY));
        }

        // ---- Toggle switch ----
        if (event.getItemStack().getItem() instanceof SwitchItem) {
            event.getToolTip().add(Component.literal("Type: SPST toggle")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Rated: " + SwitchItem.MAX_VOLTAGE + " V, "
                            + SwitchItem.MAX_CURRENT_MA + " mA")
                    .withStyle(ChatFormatting.GRAY));
        }

        // ---- Breadboards (NEW) ----
        // Breadboards are blocks, so in your inventory they're a BlockItem.
        // Check 1: is it a block item? Check 2: is that block a breadboard?
        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof BreadboardBlock board) {

            int columns = board.getColumns();  // 15 or 30
            int strips = board.getStrips();    // 1 or 2

            // Each strip has 10 rows; there's one more pair of rails (2 rows each) than strips
            int rowCount = strips * 10 + (strips + 1) * 2;

            // "30 columns × 2 strips" (says "strip" instead of "strips" when there's only 1)
            event.getToolTip().add(Component.literal("Size: " + columns + " columns × " + strips
                            + (strips == 1 ? " strip" : " strips"))
                    .withStyle(ChatFormatting.GRAY));

            // Tie points = every hole on the board
            event.getToolTip().add(Component.literal("Tie points: " + columns * rowCount)
                    .withStyle(ChatFormatting.GRAY));

            // Warning, slightly darker so it reads as a side note
            event.getToolTip().add(Component.literal("Combining boards clears circuits")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}