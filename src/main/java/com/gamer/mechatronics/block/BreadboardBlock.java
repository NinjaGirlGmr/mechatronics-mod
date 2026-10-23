package com.gamer.mechatronics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


public class BreadboardBlock extends Block{
    // A box from (0,0,0) to (16,2,16) in pixels: full width, 2 pixels tall
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public BreadboardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
