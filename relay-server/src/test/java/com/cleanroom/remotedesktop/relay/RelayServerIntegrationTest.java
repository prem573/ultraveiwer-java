package com.cleanroom.remotedesktop.relay;

import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketType;
import com.cleanroom.remotedesktop.protocol.codec.PacketDecoder;
import com.cleanroom.remotedesktop.protocol.codec.PacketEncoder;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.ChatMessage;
import com.cleanroom.remotedesktop.protocol.model.JsonPayloadUtil;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.ConnectPartnerReq;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.RegisterHostReq;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.RegisterHostResp;
import com.cleanroom.remotedesktop.security.SslContextFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RelayServerIntegrationTest {
    private RelayServer relayServer;
    private EventLoopGroup clientGroup;
    private int relayPort;

    @BeforeEach
    public void setUp() throws Exception {
        relayServer = new RelayServer("127.0.0.1", 0, true);
        relayServer.start();
        relayPort = relayServer.getBoundPort();
        clientGroup = new NioEventLoopGroup();
    }

    @AfterEach
    public void tearDown() {
        if (clientGroup != null) {
            clientGroup.shutdownGracefully();
        }
        if (relayServer != null) {
            relayServer.stop();
        }
    }

    @Test
    public void testHostRegistrationAndRelayedCommunication() throws Exception {
        final SslContext clientSsl = SslContextFactory.createClientContext();
        final BlockingQueue<Packet> hostQueue = new LinkedBlockingQueue<>();
        final BlockingQueue<Packet> viewerQueue = new LinkedBlockingQueue<>();

        // 1. Connect Host
        Channel hostChannel = connectClient("127.0.0.1", relayPort, clientSsl, hostQueue);
        RegisterHostReq regReq = new RegisterHostReq("111222333");
        hostChannel.writeAndFlush(Packet.create(PacketType.REGISTER_HOST_REQ, JsonPayloadUtil.serialize(regReq)));

        Packet hostRespPacket = hostQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(hostRespPacket, "Host should receive register response");
        assertEquals(PacketType.REGISTER_HOST_RESP, hostRespPacket.getPacketType());
        RegisterHostResp regResp = JsonPayloadUtil.deserialize(hostRespPacket.getPayload(), RegisterHostResp.class);
        assertTrue(regResp.isSuccess());
        String hostId = regResp.getHostId();
        assertNotNull(hostId);

        // 2. Connect Viewer
        Channel viewerChannel = connectClient("127.0.0.1", relayPort, clientSsl, viewerQueue);
        ConnectPartnerReq connReq = new ConnectPartnerReq(hostId);
        viewerChannel.writeAndFlush(Packet.create(PacketType.CONNECT_PARTNER_REQ, JsonPayloadUtil.serialize(connReq)));

        // 3. Host should receive CONNECT_PARTNER_REQ
        Packet incomingReq = hostQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(incomingReq, "Host should receive forwarded connect request");
        assertEquals(PacketType.CONNECT_PARTNER_REQ, incomingReq.getPacketType());

        // 4. Test Relaying chat message from Host to Viewer
        ChatMessage msg = new ChatMessage("host", "Host Operator", "Relay connection established!", System.currentTimeMillis());
        hostChannel.writeAndFlush(Packet.create(PacketType.CHAT_MESSAGE, JsonPayloadUtil.serialize(msg)));

        Packet relayedPacket = viewerQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(relayedPacket, "Viewer should receive relayed chat packet");
        assertEquals(PacketType.CHAT_MESSAGE, relayedPacket.getPacketType());
        ChatMessage receivedMsg = JsonPayloadUtil.deserialize(relayedPacket.getPayload(), ChatMessage.class);
        assertEquals("Relay connection established!", receivedMsg.getText());

        hostChannel.close();
        viewerChannel.close();
    }

    private Channel connectClient(String host, int port, SslContext sslContext, BlockingQueue<Packet> queue) throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc(), host, port));
                        ch.pipeline().addLast("decoder", new PacketDecoder());
                        ch.pipeline().addLast("encoder", new PacketEncoder());
                        ch.pipeline().addLast("handler", new SimpleChannelInboundHandler<Packet>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
                                queue.offer(msg);
                            }
                        });
                    }
                });
        return b.connect(host, port).sync().channel();
    }
}

