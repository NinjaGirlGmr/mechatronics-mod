package com.gamer.mechatronics.network;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.BreadboardBlock;
import com.gamer.mechatronics.block.BreadboardBlockEntity;
import com.gamer.mechatronics.block.Wire;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// No "value = Dist.CLIENT" here: the server must know about packets too
@EventBusSubscriber(modid = Mechatronics.MODID)
public class ModPackets {

    // Tell NeoForge about our packets when the game starts
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");  // "1" = version of our packet set
        // playToServer = this packet only goes client → server
        registrar.playToServer(AddWirePayload.TYPE, AddWirePayload.STREAM_CODEC, ModPackets::handleAddWire);
    }

    // Runs on the SERVER when an AddWire packet arrives
    private static void handleAddWire(AddWirePayload payload, IPayloadContext context) {
        Player player = context.player();   // who sent it
        Level level = player.level();
        BlockPos pos = payload.pos();
        Wire wire = payload.wire();

        // ---- Check 1: is the player close enough? (8 blocks; 8² = 64) ----
        if (player.distanceToSqr(pos.getCenter()) > 64.0) return;

        // ---- Check 2: is there really a breadboard there? ----
        if (!(level.getBlockState(pos).getBlock() instanceof BreadboardBlock board)) return;
        if (!(level.getBlockEntity(pos) instanceof BreadboardBlockEntity entity)) return;

        // ---- Check 3: are both holes actually on this board, and different? ----
        int columns = board.getColumns();
        int rowCount = board.getRowCount();
        if (!onBoard(wire.startCol(), wire.startRow(), columns, rowCount)) return;
        if (!onBoard(wire.endCol(), wire.endRow(), columns, rowCount)) return;
        if (wire.startCol() == wire.endCol() && wire.startRow() == wire.endRow()) return;

        // ---- Check 4: pay for it (creative mode is free) ----
        if (!player.isCreative() && !takeOne(player, Mechatronics.COPPER_WIRE.get())) {
            player.sendOverlayMessage(Component.literal("You need copper wire!"));
            return;
        }

        // ---- All good: add it ----
        // Rebuild the wire as BARE no matter what color the client sent.
        // Colors come later, and the server decides, not the client.
        entity.addWire(new Wire(wire.startCol(), wire.startRow(), wire.endCol(), wire.endRow(), Wire.BARE));
        player.sendOverlayMessage(Component.literal("Wire placed (" + entity.getWires().size() + " on this board)"));
    }

    // Is this hole inside the board?
    private static boolean onBoard(int col, int row, int columns, int rowCount) {
        return col >= 0 && col < columns && row >= 0 && row < rowCount;
    }

    // Finds one of this item anywhere in the inventory and removes it. Returns false if none.
    private static boolean takeOne(Player player, Item item) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);  // remove one from that stack
                return true;
            }
        }
        return false;
    }
}