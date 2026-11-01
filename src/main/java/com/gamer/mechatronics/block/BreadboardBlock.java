package com.gamer.mechatronics.block;

import com.gamer.mechatronics.menu.BreadboardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


public class BreadboardBlock extends Block implements EntityBlock {
    // A box from (0,0,0) to (16,2,16) in pixels: full width, 2 pixels tall
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    // allows expandability
    private final int columns;
    private final int strips;

    public BreadboardBlock(Properties properties, int columns, int strips) {
        super(properties);
        this.columns = columns;
        this.strips = strips;
    }

    // get files, size of board and avaliable wiring clamps
    public int getStrips() {
        return strips;
    }

    public int getColumns() {
        return columns;
    }

    // Total rows of holes: 10 per strip, plus 2 rail rows for each pair of rails
    public int getRowCount() {
        return strips * 10 + (strips + 1) * 2;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreadboardBlockEntity(pos, state);
    }


    // this method sucks but its needed
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Only the server opens menus, and it needs the server version of the player
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    // 1. How to build the server's menu, plus the window title
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new BreadboardMenu(id, inventory,
                                    ContainerLevelAccess.create(level, pos), columns, strips, pos),
                            this.getName()),
                    // 2. Extra data sent to the player's game, in this exact order
                    buf -> {
                        buf.writeVarInt(columns);
                        buf.writeVarInt(strips);
                        buf.writeBlockPos(pos);   // NEW: where this board is
                    });
        }
        return InteractionResult.SUCCESS;
    }
}
