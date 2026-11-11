package com.cleanroom.remotedesktop.relay;

import com.cleanroom.remotedesktop.security.RandomPasswordGenerator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RelaySessionManager {
    private static final Logger log = LoggerFactory.getLogger(RelaySessionManager.class);

    // Host ID -> Host Channel
    private final Map<String, Channel> registeredHosts = new ConcurrentHashMap<>();
    // Channel ID -> Host ID (for fast reverse lookup on disconnect)
    private final Map<ChannelId, String> channelToHostId = new ConcurrentHashMap<>();

    // Bidirectional paired session map: Channel A <-> Channel B
    private final Map<Channel, Channel> pairedPeers = new ConcurrentHashMap<>();

    public synchronized String registerHost(String preferredId, Channel channel) {
        String hostId = preferredId;
        if (hostId == null || hostId.trim().isEmpty() || registeredHosts.containsKey(hostId)) {
            do {
                hostId = RandomPasswordGenerator.generateRandomHostId();
            } while (registeredHosts.containsKey(hostId));
        }

        registeredHosts.put(hostId, channel);
        channelToHostId.put(channel.id(), hostId);
        log.info("Registered host ID: {} from remote address: {}", hostId, channel.remoteAddress());
        return hostId;
    }

    public Channel getHostChannel(String hostId) {
        if (hostId == null) return null;
        String cleanId = hostId.replaceAll("\\s+", "");
        Channel channel = registeredHosts.get(cleanId);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        return null;
    }

    public void pairSessions(Channel clientA, Channel clientB) {
        pairedPeers.put(clientA, clientB);
        pairedPeers.put(clientB, clientA);
        log.info("Paired sessions: {} <==> {}", clientA.remoteAddress(), clientB.remoteAddress());
    }

    public Channel getPairedPeer(Channel channel) {
        return pairedPeers.get(channel);
    }

    public void unpair(Channel channel) {
        Channel peer = pairedPeers.remove(channel);
        if (peer != null) {
            pairedPeers.remove(peer);
            log.info("Unpaired session for channel: {}", channel.remoteAddress());
        }
    }

    public void unregisterChannel(Channel channel) {
        unpair(channel);
        String hostId = channelToHostId.remove(channel.id());
        if (hostId != null) {
            registeredHosts.remove(hostId);
            log.info("Unregistered host ID: {}", hostId);
        }
    }

    public int getActiveHostCount() {
        return registeredHosts.size();
    }

    public int getActivePairedSessionCount() {
        return pairedPeers.size() / 2;
    }
}

