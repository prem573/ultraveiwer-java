package com.cleanroom.remotedesktop.client.viewer;

import com.cleanroom.remotedesktop.client.clipboard.ClipboardSyncService;
import com.cleanroom.remotedesktop.client.file.FileTransferService;
import com.cleanroom.remotedesktop.client.net.ClientNetworkManager;
import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketType;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.*;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteKeyboardEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.SystemAction;
import com.cleanroom.remotedesktop.protocol.model.JsonPayloadUtil;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.QualitySettings;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenMetadata;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTileBatch;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.*;
import com.cleanroom.remotedesktop.security.AuthCryptoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

public class ViewerSession {
    private static final Logger log = LoggerFactory.getLogger(ViewerSession.class);

    private final ClientNetworkManager networkManager;
    private final FileTransferService fileService = new FileTransferService();
    private final ClipboardSyncService clipboardService;

    private String targetPartnerId;
    private String targetPassword;
    private volatile boolean connected = false;
    private volatile boolean viewOnly = false;
    private ScreenMetadata screenMetadata;

    // Callbacks to UI
    private Consumer<SessionAccepted> onSessionAccepted;
    private Consumer<String> onSessionRejected;
    private Consumer<ScreenMetadata> onScreenMetadataReceived;
    private Consumer<ScreenTileBatch> onScreenTilesReceived;
    private Consumer<ChatMessage> onChatMessageReceived;
    private Consumer<FileListResp> onFileListReceived;
    private Runnable onSessionTerminated;

    public ViewerSession(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        this.clipboardService = new ClipboardSyncService(this::sendClipboardData);
        registerPacketHandlers();
    }

    private void registerPacketHandlers() {
        networkManager.addListener(PacketType.AUTH_CHALLENGE, this::onAuthChallenge);
        networkManager.addListener(PacketType.SESSION_ACCEPTED, this::onSessionAcceptedPacket);
        networkManager.addListener(PacketType.SESSION_REJECTED, this::onSessionRejectedPacket);
        networkManager.addListener(PacketType.SESSION_TERMINATE, this::onSessionTerminatePacket);
        networkManager.addListener(PacketType.SCREEN_METADATA, this::onScreenMetadataPacket);
        networkManager.addListener(PacketType.SCREEN_TILE_BATCH, this::onScreenTileBatchPacket);
        networkManager.addListener(PacketType.CLIPBOARD_DATA, this::onClipboardDataPacket);
        networkManager.addListener(PacketType.CHAT_MESSAGE, this::onChatMessagePacket);
        networkManager.addListener(PacketType.FILE_LIST_RESP, this::onFileListRespPacket);
        networkManager.addListener(PacketType.FILE_CHUNK, this::onFileChunkPacket);
    }

    public void connectToPartner(String partnerId, String password) {
        this.targetPartnerId = partnerId.replaceAll("\\s+", "");
        this.targetPassword = password != null ? password.trim() : "";

        log.info("Requesting connection to Partner ID: {}", targetPartnerId);
        ConnectPartnerReq req = new ConnectPartnerReq(targetPartnerId);
        networkManager.sendPacket(Packet.create(PacketType.CONNECT_PARTNER_REQ, JsonPayloadUtil.serialize(req)));
    }

    private void onAuthChallenge(Packet packet) {
        AuthChallenge challenge = JsonPayloadUtil.deserialize(packet.getPayload(), AuthChallenge.class);
        String nonce = challenge.getNonce();
        String hmac = AuthCryptoHelper.calculateHmac(targetPassword, nonce);

        log.info("Submitting auth response proof to host...");
        AuthResponse resp = new AuthResponse(targetPartnerId, nonce, hmac);
        networkManager.sendPacket(Packet.create(PacketType.AUTH_RESPONSE, JsonPayloadUtil.serialize(resp)));
    }

    private void onSessionAcceptedPacket(Packet packet) {
        SessionAccepted accepted = JsonPayloadUtil.deserialize(packet.getPayload(), SessionAccepted.class);
        this.connected = true;
        this.viewOnly = accepted.isViewOnly();
        log.info("Session accepted by host: {}", accepted.getPartnerId());

        clipboardService.start();
        if (onSessionAccepted != null) {
            onSessionAccepted.accept(accepted);
        }
    }

    private void onSessionRejectedPacket(Packet packet) {
        SessionRejected rejected = JsonPayloadUtil.deserialize(packet.getPayload(), SessionRejected.class);
        this.connected = false;
        log.warn("Session rejected by host: {}", rejected.getReason());
        if (onSessionRejected != null) {
            onSessionRejected.accept(rejected.getReason());
        }
    }

    private void onSessionTerminatePacket(Packet packet) {
        log.info("Session terminated by host or relay.");
        this.connected = false;
        clipboardService.stop();
        if (onSessionTerminated != null) {
            onSessionTerminated.run();
        }
    }

    public void disconnect() {
        if (connected) {
            log.info("Viewer terminating session with Partner ID: {}", targetPartnerId);
            SessionTerminate term = new SessionTerminate("Viewer closed the session.");
            networkManager.sendPacket(Packet.create(PacketType.SESSION_TERMINATE, JsonPayloadUtil.serialize(term)));
            connected = false;
            clipboardService.stop();
        }
    }

    private void onScreenMetadataPacket(Packet packet) {
        ScreenMetadata metadata = JsonPayloadUtil.deserialize(packet.getPayload(), ScreenMetadata.class);
        this.screenMetadata = metadata;
        if (onScreenMetadataReceived != null) {
            onScreenMetadataReceived.accept(metadata);
        }
    }

    private void onScreenTileBatchPacket(Packet packet) {
        ScreenTileBatch batch = JsonPayloadUtil.deserialize(packet.getPayload(), ScreenTileBatch.class);
        if (onScreenTilesReceived != null) {
            onScreenTilesReceived.accept(batch);
        }
    }

    public void sendMouseEvent(RemoteMouseEvent event) {
        if (connected && !viewOnly) {
            networkManager.sendPacket(Packet.create(PacketType.MOUSE_EVENT, JsonPayloadUtil.serialize(event)));
        }
    }

    public void sendKeyboardEvent(RemoteKeyboardEvent event) {
        if (connected && !viewOnly) {
            networkManager.sendPacket(Packet.create(PacketType.KEYBOARD_EVENT, JsonPayloadUtil.serialize(event)));
        }
    }

    public void sendSystemAction(SystemAction action) {
        if (connected && !viewOnly) {
            networkManager.sendPacket(Packet.create(PacketType.SYSTEM_ACTION, JsonPayloadUtil.serialize(action)));
        }
    }

    public void sendQualitySettings(QualitySettings settings) {
        if (connected) {
            networkManager.sendPacket(Packet.create(PacketType.QUALITY_SETTINGS, JsonPayloadUtil.serialize(settings)));
        }
    }

    private void onClipboardDataPacket(Packet packet) {
        ClipboardData data = JsonPayloadUtil.deserialize(packet.getPayload(), ClipboardData.class);
        clipboardService.onRemoteClipboardReceived(data);
    }

    private void sendClipboardData(ClipboardData data) {
        if (connected) {
            networkManager.sendPacket(Packet.create(PacketType.CLIPBOARD_DATA, JsonPayloadUtil.serialize(data)));
        }
    }

    private void onChatMessagePacket(Packet packet) {
        ChatMessage msg = JsonPayloadUtil.deserialize(packet.getPayload(), ChatMessage.class);
        if (onChatMessageReceived != null) {
            onChatMessageReceived.accept(msg);
        }
    }

    public void sendChatMessage(String text) {
        if (connected && text != null && !text.trim().isEmpty()) {
            ChatMessage msg = new ChatMessage("viewer", "Viewer", text.trim(), System.currentTimeMillis());
            networkManager.sendPacket(Packet.create(PacketType.CHAT_MESSAGE, JsonPayloadUtil.serialize(msg)));
        }
    }

    public void requestRemoteFileList(String path) {
        if (connected) {
            FileListReq req = new FileListReq(path);
            networkManager.sendPacket(Packet.create(PacketType.FILE_LIST_REQ, JsonPayloadUtil.serialize(req)));
        }
    }

    private void onFileListRespPacket(Packet packet) {
        FileListResp resp = JsonPayloadUtil.deserialize(packet.getPayload(), FileListResp.class);
        if (onFileListReceived != null) {
            onFileListReceived.accept(resp);
        }
    }

    public void sendFileToRemote(File localFile, String remoteTargetPath, Consumer<Double> progressCallback) {
        if (connected) {
            new Thread(() -> {
                fileService.sendFile(localFile, remoteTargetPath, chunk -> {
                    networkManager.sendPacket(Packet.create(PacketType.FILE_CHUNK, JsonPayloadUtil.serialize(chunk)));
                }, progressCallback);
            }, "Viewer-File-Sender").start();
        }
    }

    private void onFileChunkPacket(Packet packet) {
        FileChunk chunk = JsonPayloadUtil.deserialize(packet.getPayload(), FileChunk.class);
        fileService.receiveChunk(chunk);
    }

    public boolean isConnected() { return connected; }
    public boolean isViewOnly() { return viewOnly; }
    public void setViewOnly(boolean viewOnly) { this.viewOnly = viewOnly; }
    public ScreenMetadata getScreenMetadata() { return screenMetadata; }
    public String getTargetPartnerId() { return targetPartnerId; }

    public void setOnSessionAccepted(Consumer<SessionAccepted> onSessionAccepted) { this.onSessionAccepted = onSessionAccepted; }
    public void setOnSessionRejected(Consumer<String> onSessionRejected) { this.onSessionRejected = onSessionRejected; }
    public void setOnScreenMetadataReceived(Consumer<ScreenMetadata> onScreenMetadataReceived) { this.onScreenMetadataReceived = onScreenMetadataReceived; }
    public void setOnScreenTilesReceived(Consumer<ScreenTileBatch> onScreenTilesReceived) { this.onScreenTilesReceived = onScreenTilesReceived; }
    public void setOnChatMessageReceived(Consumer<ChatMessage> onChatMessageReceived) { this.onChatMessageReceived = onChatMessageReceived; }
    public void setOnFileListReceived(Consumer<FileListResp> onFileListReceived) { this.onFileListReceived = onFileListReceived; }
    public void setOnSessionTerminated(Runnable onSessionTerminated) { this.onSessionTerminated = onSessionTerminated; }
}

