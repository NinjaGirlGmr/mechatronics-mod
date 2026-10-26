package com.gamer.mechatronics.menu;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.BreadboardBlock;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;



public class BreadboardMenu extends AbstractContainerMenu {
    // Remembers which breadboard this menu belongs to (server side)
    private final ContainerLevelAccess access;
    private final int columns;


    // Client side: the client doesn't need to know where the board is
    public BreadboardMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL,buf.readVarInt());
    }

    // Server: knows everything directly
    public BreadboardMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int columns) {
        super(Mechatronics.BREADBOARD_MENU.get(), containerId);
        this.access = access;
        this.columns = columns;
    }
    public int getColumns() {
        return columns;
    }


    // Close the menu if the player walks away or the board is broken
    // Valid while the block is still any breadboard and the player is within 8 blocks
    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).getBlock() instanceof BreadboardBlock
                        && player.distanceToSqr(pos.getCenter()) <= 64.0, true);
    }

    // Shift-click behavior. No item slots yet, so nothing moves.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}