package com.cleanroom.remotedesktop.protocol;

import com.cleanroom.remotedesktop.protocol.codec.PacketDecoder;
import com.cleanroom.remotedesktop.protocol.codec.PacketEncoder;
import com.cleanroom.remotedesktop.protocol.model.JsonPayloadUtil;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.RegisterHostResp;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolCodecTest {

    @Test
    public void testEncodeAndDecodePacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(), new PacketEncoder());

        byte[] payload = "Hello Remote Desktop Protocol!".getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(PacketType.CHAT_MESSAGE, payload);

        assertTrue(channel.writeOutbound(packet));
        Object encodedByteBuf = channel.readOutbound();
        assertNotNull(encodedByteBuf);

        assertTrue(channel.writeInbound(encodedByteBuf));
        Packet decoded = channel.readInbound();
        assertNotNull(decoded);

        assertEquals(PacketType.CHAT_MESSAGE, decoded.getPacketType());
        assertArrayEquals(payload, decoded.getPayload());
        assertEquals(packet.getHeader().getSequenceId(), decoded.getHeader().getSequenceId());
    }

    @Test
    public void testJsonPayloadRoundTrip() {
        RegisterHostResp original = new RegisterHostResp(true, "123456789", "Host registered successfully");
        byte[] bytes = JsonPayloadUtil.serialize(original);

        RegisterHostResp deserialized = JsonPayloadUtil.deserialize(bytes, RegisterHostResp.class);
        assertTrue(deserialized.isSuccess());
        assertEquals("123456789", deserialized.getHostId());
        assertEquals("Host registered successfully", deserialized.getMessage());
    }
}
