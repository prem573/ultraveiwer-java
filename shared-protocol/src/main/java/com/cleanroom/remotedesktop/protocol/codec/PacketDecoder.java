package com.cleanroom.remotedesktop.protocol.codec;

import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketHeader;
import com.cleanroom.remotedesktop.protocol.PacketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class PacketDecoder extends LengthFieldBasedFrameDecoder {

    // maxFrameLength: 32 MB (for large tile batches/file chunks)
    // lengthFieldOffset: 0
    // lengthFieldLength: 4
    // lengthAdjustment: 0
    // initialBytesToStrip: 4
    public PacketDecoder() {
        this(32 * 1024 * 1024);
    }

    public PacketDecoder(int maxFrameLength) {
        super(maxFrameLength, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            if (frame.readableBytes() < PacketHeader.HEADER_SIZE) {
                throw new IllegalArgumentException("Frame size too small: " + frame.readableBytes());
            }

            short magic = frame.readShort();
            if (magic != PacketHeader.MAGIC) {
                throw new IllegalArgumentException("Invalid magic: 0x" + Integer.toHexString(magic & 0xFFFF));
            }

            short typeCode = frame.readShort();
            PacketType type = PacketType.fromCode(typeCode);
            int sequenceId = frame.readInt();
            long timestamp = frame.readLong();
            int payloadLength = frame.readInt();

            if (frame.readableBytes() < payloadLength) {
                throw new IllegalArgumentException("Readable bytes (" + frame.readableBytes() +
                        ") less than payload length (" + payloadLength + ")");
            }

            byte[] payload = new byte[payloadLength];
            if (payloadLength > 0) {
                frame.readBytes(payload);
            }

            PacketHeader header = new PacketHeader(magic, type, sequenceId, timestamp, payloadLength);
            return new Packet(header, payload);
        } finally {
            frame.release();
        }
    }
}
