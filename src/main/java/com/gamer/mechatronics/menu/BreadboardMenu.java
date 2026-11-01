package com.gamer.mechatronics.menu;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.BreadboardBlock;
import net.minecraft.core.BlockPos;
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
    private final int strips;
    private final BlockPos pos;

    // Client: read in the SAME ORDER the server wrote: columns, strips, pos
    public BreadboardMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL,
                buf.readVarInt(), buf.readVarInt(), buf.readBlockPos());
    }


    // Server
    public BreadboardMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access,
                          int columns, int strips, BlockPos pos) {
        super(Mechatronics.BREADBOARD_MENU.get(), containerId);
        this.access = access;
        this.columns = columns;
        this.strips = strips;
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
    public int getColumns() {
        return columns;
    }

    public int getStrips() {
        return strips;
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