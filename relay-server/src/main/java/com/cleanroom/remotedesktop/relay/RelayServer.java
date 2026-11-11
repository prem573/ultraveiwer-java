package com.cleanroom.remotedesktop.relay;

import com.cleanroom.remotedesktop.protocol.codec.PacketDecoder;
import com.cleanroom.remotedesktop.protocol.codec.PacketEncoder;
import com.cleanroom.remotedesktop.security.SslContextFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RelayServer {
    private static final Logger log = LoggerFactory.getLogger(RelayServer.class);

    private final String host;
    private final int port;
    private final boolean useTls;
    private final RelaySessionManager sessionManager = new RelaySessionManager();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private int boundPort;

    public RelayServer(String host, int port, boolean useTls) {
        this.host = host;
        this.port = port;
        this.useTls = useTls;
    }

    public synchronized void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        final SslContext sslContext = useTls ? SslContextFactory.createServerContext() : null;

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) {
                            ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc()));
                        }
                        ch.pipeline().addLast("decoder", new PacketDecoder());
                        ch.pipeline().addLast("encoder", new PacketEncoder());
                        ch.pipeline().addLast("handler", new RelayServerHandler(sessionManager));
                    }
                });

        serverChannel = b.bind(host, port).sync().channel();
        boundPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
        log.info("Clean-Room Relay Server started on {}:{} (TLS={})", host, boundPort, useTls);
    }

    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Relay Server stopped.");
    }

    public int getBoundPort() {
        return boundPort;
    }

    public RelaySessionManager getSessionManager() {
        return sessionManager;
    }
}

