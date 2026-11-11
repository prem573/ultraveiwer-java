package com.cleanroom.remotedesktop.client.net;

import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketType;
import com.cleanroom.remotedesktop.protocol.codec.PacketDecoder;
import com.cleanroom.remotedesktop.protocol.codec.PacketEncoder;
import com.cleanroom.remotedesktop.security.SslContextFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientNetworkManager {
    private static final Logger log = LoggerFactory.getLogger(ClientNetworkManager.class);

    private final String relayHost;
    private final int relayPort;
    private final boolean useTls;

    private EventLoopGroup workerGroup;
    private Channel channel;
    private final Map<PacketType, List<Consumer<Packet>>> packetListeners = new ConcurrentHashMap<>();
    private final List<Consumer<Boolean>> connectionStateListeners = new CopyOnWriteArrayList<>();

    public ClientNetworkManager(String relayHost, int relayPort, boolean useTls) {
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.useTls = useTls;
    }

    public synchronized void connect() throws Exception {
        if (channel != null && channel.isActive()) {
            return;
        }

        workerGroup = new NioEventLoopGroup();
        final SslContext sslContext = useTls ? SslContextFactory.createClientContext() : null;

        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) {
                            ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc(), relayHost, relayPort));
                        }
                        ch.pipeline().addLast("decoder", new PacketDecoder());
                        ch.pipeline().addLast("encoder", new PacketEncoder());
                        ch.pipeline().addLast("handler", new SimpleChannelInboundHandler<Packet>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
                                dispatchPacket(msg);
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                notifyConnectionState(true);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                notifyConnectionState(false);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.warn("Client network error: {}", cause.getMessage());
                                ctx.close();
                            }
                        });
                    }
                });

        channel = b.connect(relayHost, relayPort).sync().channel();
        log.info("Connected to Relay at {}:{} (TLS={})", relayHost, relayPort, useTls);
    }

    public void sendPacket(Packet packet) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);
        } else {
            log.warn("Cannot send packet, channel is not active: {}", packet.getPacketType());
        }
    }

    public void addListener(PacketType type, Consumer<Packet> listener) {
        packetListeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void removeListener(PacketType type, Consumer<Packet> listener) {
        List<Consumer<Packet>> list = packetListeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void addConnectionStateListener(Consumer<Boolean> listener) {
        connectionStateListeners.add(listener);
    }

    private void dispatchPacket(Packet packet) {
        List<Consumer<Packet>> listeners = packetListeners.get(packet.getPacketType());
        if (listeners != null && !listeners.isEmpty()) {
            for (Consumer<Packet> l : listeners) {
                try {
                    l.accept(packet);
                } catch (Exception e) {
                    log.error("Error in packet listener for {}", packet.getPacketType(), e);
                }
            }
        } else {
            log.debug("No listener registered for packet type: {}", packet.getPacketType());
        }
    }

    private void notifyConnectionState(boolean connected) {
        for (Consumer<Boolean> listener : connectionStateListeners) {
            try {
                listener.accept(connected);
            } catch (Exception e) {
                log.error("Error in connection state listener", e);
            }
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    public synchronized void disconnect() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        notifyConnectionState(false);
    }
}
