package com.gamer.mechatronics.network;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.Wire;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from the screen to the server: "please add this wire to the board at pos."
 * A record holds the packet's contents: which board, and which wire.
 */
public record AddWirePayload(BlockPos pos, Wire wire) implements CustomPacketPayload {

    // The packet's unique name: "mechatronics:add_wire"
    public static final Type<AddWirePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Mechatronics.MODID, "add_wire"));

    // How to send it: the position, then the wire (using Wire's own STREAM_CODEC)
    public static final StreamCodec<ByteBuf, AddWirePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AddWirePayload::pos,
            Wire.STREAM_CODEC, AddWirePayload::wire,
            AddWirePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}