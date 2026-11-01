package com.gamer.mechatronics.block;

import com.gamer.mechatronics.Mechatronics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BreadboardBlockEntity extends BlockEntity {
    // Every wire on this board
    private final List<Wire> wires = new ArrayList<>();

    public BreadboardBlockEntity(BlockPos pos, BlockState state) {
        super(Mechatronics.BREADBOARD_BE.get(), pos, state);
    }

    public void addWire(Wire wire) {
        wires.add(wire);
        setChanged();  // remember to save
        // NEW: on the server, tell every nearby player's game that this board changed.
        // That triggers getUpdatePacket() below, which sends them the new data.
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Read-only view: other code can look at the wires but not change the list directly
    public List<Wire> getWires() {
        return Collections.unmodifiableList(wires);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("wires", Wire.CODEC.listOf(), wires);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wires.clear();
        input.read("wires", Wire.CODEC.listOf()).ifPresent(wires::addAll);
    }

    // NEW: what to send a player when they first load this board (for example, walking up to it)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);  // the same data we save, wires included
    }

    // NEW: the packet sent whenever sendBlockUpdated is called
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}