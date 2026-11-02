package com.gamer.mechatronics.client;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.BreadboardBlock;
import com.gamer.mechatronics.block.Wire;
import com.gamer.mechatronics.part.BoardPart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

// Adds electrical info to our items' tooltips. Client only: servers don't draw tooltips.
@EventBusSubscriber(modid = Mechatronics.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {

        // ---- Any breadboard part (LEDs, resistors, switches, and any future or addon part) ----
        // Each part provides its own stats through addStats, so this one block covers all of them.
        if (event.getItemStack().getItem() instanceof BoardPart part) {
            // Every stat line gets the same gray style, so all parts look consistent
            part.addStats(line -> event.getToolTip().add(line.copy().withStyle(ChatFormatting.GRAY)));

            // Shared info every part has
            String direction = part.polarized() ? ", + leg first" : "";
            event.getToolTip().add(Component.literal("Breadboard part: " + part.legs() + " legs" + direction)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        // ---- Breadboards ----
        // Breadboards are blocks, so in your inventory they're a BlockItem.
        // Check 1: is it a block item? Check 2: is that block a breadboard?
        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof BreadboardBlock board) {

            int columns = board.getColumns();  // 15 or 30
            int strips = board.getStrips();    // 1 or 2

            event.getToolTip().add(Component.literal("Size: " + columns + " columns × " + strips
                            + (strips == 1 ? " strip" : " strips"))
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Tie points: " + columns * board.getRowCount())
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Combining boards clears circuits")
                    .withStyle(ChatFormatting.DARK_GRAY));

            // If this board carries a circuit, say how big it is
            List<Wire> wires = event.getItemStack().get(Mechatronics.WIRES.get());
            if (wires != null) {
                event.getToolTip().add(Component.literal("Circuit: " + wires.size() + " wires")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}