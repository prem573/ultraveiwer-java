package com.cleanroom.remotedesktop.protocol;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Packet {
    private static final AtomicInteger SEQ_GENERATOR = new AtomicInteger(1);

    private final PacketHeader header;
    private final byte[] payload;

    public Packet(PacketHeader header, byte[] payload) {
        this.header = Objects.requireNonNull(header, "header cannot be null");
        this.payload = payload != null ? payload : new byte[0];
    }

    public static Packet create(PacketType type, byte[] payload) {
        int seq = SEQ_GENERATOR.getAndIncrement();
        long now = System.currentTimeMillis();
        int len = payload != null ? payload.length : 0;
        PacketHeader header = new PacketHeader(type, seq, now, len);
        return new Packet(header, payload);
    }

    public PacketHeader getHeader() {
        return header;
    }

    public PacketType getPacketType() {
        return header.getPacketType();
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "header=" + header +
                ", payloadBytes=" + (payload != null ? payload.length : 0) +
                '}';
    }
}
