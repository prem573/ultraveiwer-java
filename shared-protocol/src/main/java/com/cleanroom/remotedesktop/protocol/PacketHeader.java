package com.cleanroom.remotedesktop.protocol;

import java.util.Objects;

public class PacketHeader {
    public static final short MAGIC = 0x5556; // "UV"
    public static final int HEADER_SIZE = 20; // 2(magic) + 2(type) + 4(seq) + 8(time) + 4(len)

    private final short magic;
    private final PacketType packetType;
    private final int sequenceId;
    private final long timestamp;
    private final int payloadLength;

    public PacketHeader(PacketType packetType, int sequenceId, long timestamp, int payloadLength) {
        this(MAGIC, packetType, sequenceId, timestamp, payloadLength);
    }

    public PacketHeader(short magic, PacketType packetType, int sequenceId, long timestamp, int payloadLength) {
        this.magic = magic;
        this.packetType = Objects.requireNonNull(packetType, "packetType cannot be null");
        this.sequenceId = sequenceId;
        this.timestamp = timestamp;
        this.payloadLength = payloadLength;
    }

    public short getMagic() {
        return magic;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public String toString() {
        return "PacketHeader{" +
                "packetType=" + packetType +
                ", sequenceId=" + sequenceId +
                ", timestamp=" + timestamp +
                ", payloadLength=" + payloadLength +
                '}';
    }
}
