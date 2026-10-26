package com.gamer.mechatronics.menu;

import com.gamer.mechatronics.Mechatronics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class BreadboardMenu extends AbstractContainerMenu {
    // Remembers which breadboard this menu belongs to (server side)
    private final ContainerLevelAccess access;

    // Client side: the client doesn't need to know where the board is
    public BreadboardMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    // Server side: knows the board's world and position
    public BreadboardMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(Mechatronics.BREADBOARD_MENU.get(), containerId);
        this.access = access;
    }

    // Close the menu if the player walks away or the board is broken
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Mechatronics.BREADBOARD.get());
    }

    // Shift-click behavior. No item slots yet, so nothing moves.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}