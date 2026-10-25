package com.gamer.mechatronics.block;

import com.gamer.mechatronics.Mechatronics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BreadboardBlockEntity extends BlockEntity {
    // Temporary test data. Later this becomes the circuit.
    private int clicks = 0;

    public BreadboardBlockEntity(BlockPos pos, BlockState state) {
        super(Mechatronics.BREADBOARD_BE.get(), pos, state);
    }

    public int addClick() {
        clicks++;
        setChanged();  // "my data changed, remember to save me"
        return clicks;
    }

    // Writing to the save file
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("clicks", clicks);
    }

    // Reading back from the save file
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clicks = input.getIntOr("clicks", 0);
    }
}