package com.gamer.mechatronics.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One wire on a breadboard: two holes and a color.
 * Holes use the same column/row numbers as the screen (both start at 0).
 * color: a DyeColor id (0–15), or BARE (-1) for uninsulated copper.
 */
public record Wire(int startCol, int startRow, int endCol, int endRow, int color) {

    public static final int BARE = -1;

    public boolean isBare() {
        return color == BARE;
    }

    // Describes how to turn a Wire into saved data and back again
    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("start_col").forGetter(Wire::startCol),
            Codec.INT.fieldOf("start_row").forGetter(Wire::startRow),
            Codec.INT.fieldOf("end_col").forGetter(Wire::endCol),
            Codec.INT.fieldOf("end_row").forGetter(Wire::endRow),
            Codec.INT.fieldOf("color").forGetter(Wire::color)
    ).apply(instance, Wire::new));

    // Describes how to send a Wire over the network (packets), in this exact order
    public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Wire::startCol,
            ByteBufCodecs.VAR_INT, Wire::startRow,
            ByteBufCodecs.VAR_INT, Wire::endCol,
            ByteBufCodecs.VAR_INT, Wire::endRow,
            ByteBufCodecs.VAR_INT, Wire::color,
            Wire::new);
}