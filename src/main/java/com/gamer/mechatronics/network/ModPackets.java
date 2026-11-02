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

/**
 * Registers our packets and handles them on the server.
 * No "value = Dist.CLIENT" here: the server must know about packets too.
 */
@EventBusSubscriber(modid = Mechatronics.MODID)
public class ModPackets {

    // How close a player must be to a board to change it: 8 blocks (8 squared = 64)
    private static final double MAX_DISTANCE_SQUARED = 64.0;

    // =====================================================================
    // REGISTRATION: tell NeoForge about every packet, once, at startup
    // =====================================================================
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");  // "1" = version of our packet set
        // playToServer = these packets only go client -> server
        registrar.playToServer(AddWirePayload.TYPE, AddWirePayload.STREAM_CODEC, ModPackets::handleAddWire);
        registrar.playToServer(RemoveWirePayload.TYPE, RemoveWirePayload.STREAM_CODEC, ModPackets::handleRemoveWire);
    }

    // =====================================================================
    // ADD A WIRE (runs on the SERVER)
    // =====================================================================
    private static void handleAddWire(AddWirePayload payload, IPayloadContext context) {
        Player player = context.player();  // who sent it
        Wire wire = payload.wire();

        // ---- Checks 1 + 2: close enough, and really a breadboard? ----
        BreadboardBlockEntity entity = findBoard(player, payload.pos());
        if (entity == null) return;
        BreadboardBlock board = (BreadboardBlock) entity.getBlockState().getBlock(); // findBoard already checked this is safe

        // ---- Check 3: are both holes on this board, and different? ----
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
        // Rebuilt as BARE no matter what color the client sent: the server decides colors.
        entity.addWire(new Wire(wire.startCol(), wire.startRow(), wire.endCol(), wire.endRow(), Wire.BARE));
    }

    // =====================================================================
    // REMOVE A WIRE (runs on the SERVER)
    // =====================================================================
    private static void handleRemoveWire(RemoveWirePayload payload, IPayloadContext context) {
        Player player = context.player();

        // ---- Checks 1 + 2: close enough, and really a breadboard? ----
        BreadboardBlockEntity entity = findBoard(player, payload.pos());
        if (entity == null) return;

        // ---- Remove it. If that wire isn't actually on the board, nothing happens. ----
        // This also protects against fake packets: you can only get copper back
        // for a wire that really existed.
        if (!entity.removeWire(payload.wire())) return;

        // ---- Give the copper back (creative players didn't pay, so they get nothing) ----
        if (!player.isCreative()) {
            giveOrDrop(player, new ItemStack(Mechatronics.COPPER_WIRE.get()));
        }
    }

    // =====================================================================
    // SHARED HELPERS
    // Both handlers need the same checks, so they live here once.
    // =====================================================================

    /**
     * Checks 1 + 2: the player is close enough, and there's a breadboard with a block entity at pos.
     * Returns the board's block entity if everything is fine, or null if anything is wrong.
     */
    private static BreadboardBlockEntity findBoard(Player player, BlockPos pos) {
        if (player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQUARED) return null;
        Level level = player.level();
        if (!(level.getBlockState(pos).getBlock() instanceof BreadboardBlock)) return null;
        if (!(level.getBlockEntity(pos) instanceof BreadboardBlockEntity entity)) return null;
        return entity;
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

    // Puts an item in the player's inventory. If it's full, drops it at their feet instead.
    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {   // add() returns false if there was no room
            player.drop(stack, false);             // false = drop it right where they stand, not thrown
        }
    }
}