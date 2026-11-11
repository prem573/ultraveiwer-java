package com.cleanroom.remotedesktop.relay;

import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketType;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.HeartbeatPong;
import com.cleanroom.remotedesktop.protocol.model.JsonPayloadUtil;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.ConnectPartnerReq;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.RegisterHostReq;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.RegisterHostResp;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.SessionRejected;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.SessionTerminate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelayServerHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger log = LoggerFactory.getLogger(RelayServerHandler.class);

    private final RelaySessionManager sessionManager;

    public RelayServerHandler(RelaySessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        PacketType type = packet.getPacketType();
        Channel channel = ctx.channel();

        switch (type) {
            case REGISTER_HOST_REQ -> handleRegisterHost(channel, packet);
            case CONNECT_PARTNER_REQ -> handleConnectPartner(channel, packet);
            case HEARTBEAT_PING -> handleHeartbeat(channel, packet);
            case SESSION_TERMINATE -> handleSessionTerminate(channel, packet);
            default -> forwardToPairedPeer(channel, packet);
        }
    }

    private void handleRegisterHost(Channel channel, Packet packet) {
        try {
            RegisterHostReq req = JsonPayloadUtil.deserialize(packet.getPayload(), RegisterHostReq.class);
            String assignedId = sessionManager.registerHost(req.getPreferredId(), channel);

            RegisterHostResp resp = new RegisterHostResp(true, assignedId, "Host registration successful");
            byte[] payload = JsonPayloadUtil.serialize(resp);
            channel.writeAndFlush(Packet.create(PacketType.REGISTER_HOST_RESP, payload));
        } catch (Exception e) {
            log.error("Error processing host registration", e);
            RegisterHostResp resp = new RegisterHostResp(false, null, "Failed to register host: " + e.getMessage());
            channel.writeAndFlush(Packet.create(PacketType.REGISTER_HOST_RESP, JsonPayloadUtil.serialize(resp)));
        }
    }

    private void handleConnectPartner(Channel channel, Packet packet) {
        try {
            ConnectPartnerReq req = JsonPayloadUtil.deserialize(packet.getPayload(), ConnectPartnerReq.class);
            String targetId = req.getPartnerId();

            Channel hostChannel = sessionManager.getHostChannel(targetId);
            if (hostChannel == null || !hostChannel.isActive()) {
                log.warn("Partner ID {} not found or inactive", targetId);
                SessionRejected rejected = new SessionRejected("Partner ID (" + targetId + ") is offline or does not exist.");
                channel.writeAndFlush(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
                return;
            }

            if (hostChannel.id().equals(channel.id())) {
                SessionRejected rejected = new SessionRejected("Cannot connect to your own Host ID.");
                channel.writeAndFlush(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
                return;
            }

            // Pair viewer and host
            sessionManager.pairSessions(channel, hostChannel);

            // Forward connection request to the host
            hostChannel.writeAndFlush(packet);
            log.info("Forwarded CONNECT_PARTNER_REQ to host ID {}", targetId);
        } catch (Exception e) {
            log.error("Error connecting partner", e);
            SessionRejected rejected = new SessionRejected("Relay error: " + e.getMessage());
            channel.writeAndFlush(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
        }
    }

    private void handleHeartbeat(Channel channel, Packet packet) {
        HeartbeatPong pong = new HeartbeatPong(packet.getHeader().getTimestamp());
        channel.writeAndFlush(Packet.create(PacketType.HEARTBEAT_PONG, JsonPayloadUtil.serialize(pong)));
    }

    private void handleSessionTerminate(Channel channel, Packet packet) {
        Channel peer = sessionManager.getPairedPeer(channel);
        sessionManager.unpair(channel);

        if (peer != null && peer.isActive()) {
            peer.writeAndFlush(packet);
        }
    }

    private void forwardToPairedPeer(Channel channel, Packet packet) {
        Channel peer = sessionManager.getPairedPeer(channel);
        if (peer != null && peer.isActive()) {
            peer.writeAndFlush(packet);
        } else {
            log.debug("No active paired peer for packet: {}", packet.getPacketType());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        Channel peer = sessionManager.getPairedPeer(channel);
        if (peer != null && peer.isActive()) {
            SessionTerminate terminate = new SessionTerminate("Remote partner has disconnected.");
            peer.writeAndFlush(Packet.create(PacketType.SESSION_TERMINATE, JsonPayloadUtil.serialize(terminate)));
        }

        sessionManager.unregisterChannel(channel);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Relay handler exception: {}", cause.getMessage());
        ctx.close();
    }
}
