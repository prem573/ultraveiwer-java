package com.cleanroom.remotedesktop.protocol;

public enum PacketType {
    // Registration & Session Setup
    REGISTER_HOST_REQ(0x0001),
    REGISTER_HOST_RESP(0x0002),
    CONNECT_PARTNER_REQ(0x0003),
    AUTH_CHALLENGE(0x0004),
    AUTH_RESPONSE(0x0005),
    CONSENT_PROMPT(0x0006),
    SESSION_ACCEPTED(0x0007),
    SESSION_REJECTED(0x0008),
    SESSION_TERMINATE(0x0009),

    // Screen Streaming & Quality
    SCREEN_METADATA(0x0010),
    SCREEN_TILE_BATCH(0x0011),
    QUALITY_SETTINGS(0x0012),

    // Input Injection
    MOUSE_EVENT(0x0020),
    KEYBOARD_EVENT(0x0021),
    SYSTEM_ACTION(0x0022),

    // Clipboard
    CLIPBOARD_DATA(0x0030),

    // Chat
    CHAT_MESSAGE(0x0040),

    // File Transfer
    FILE_LIST_REQ(0x0050),
    FILE_LIST_RESP(0x0051),
    FILE_CHUNK(0x0052),
    FILE_ACK(0x0053),

    // Keepalive
    HEARTBEAT_PING(0x0090),
    HEARTBEAT_PONG(0x0091);

    private final short code;

    PacketType(int code) {
        this.code = (short) code;
    }

    public short getCode() {
        return code;
    }

    public static PacketType fromCode(short code) {
        for (PacketType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown packet code: 0x" + Integer.toHexString(code & 0xFFFF));
    }
}

