package com.gamer.mechatronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

    public BreadboardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreadboardBlockEntity(pos, state);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Only the server changes data; the client just displays it
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BreadboardBlockEntity board) {
            int clicks = board.addClick();
            player.sendOverlayMessage(Component.literal("Breadboard clicked " + clicks + " times"));
        }
        return InteractionResult.SUCCESS;
    }
}
