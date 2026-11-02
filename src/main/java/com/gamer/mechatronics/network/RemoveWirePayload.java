package com.gamer.mechatronics.network;

import com.gamer.mechatronics.Mechatronics;
import com.gamer.mechatronics.block.Wire;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from the screen to the server: "please remove this wire from the board at pos."
 * Same shape as AddWirePayload: which board, and which wire.
 */
public record RemoveWirePayload(BlockPos pos, Wire wire) implements CustomPacketPayload {

    // The packet's unique name: "mechatronics:remove_wire"
    public static final CustomPacketPayload.Type<RemoveWirePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mechatronics.MODID, "remove_wire"));

    // How to send it: the position, then the wire
    public static final StreamCodec<ByteBuf, RemoveWirePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveWirePayload::pos,
            Wire.STREAM_CODEC, RemoveWirePayload::wire,
            RemoveWirePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}