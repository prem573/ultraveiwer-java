package com.cleanroom.remotedesktop.protocol.codec;

import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        PacketHeader header = msg.getHeader();
        byte[] payload = msg.getPayload();
        int payloadLen = payload != null ? payload.length : 0;
        int frameLength = PacketHeader.HEADER_SIZE + payloadLen;

        // Total frame length (excluding the 4-byte length field itself)
        out.writeInt(frameLength);
        out.writeShort(header.getMagic());
        out.writeShort(header.getPacketType().getCode());
        out.writeInt(header.getSequenceId());
        out.writeLong(header.getTimestamp());
        out.writeInt(payloadLen);

        if (payloadLen > 0) {
            out.writeBytes(payload);
        }
    }
}

