package com.cleanroom.remotedesktop.client.host;

import com.cleanroom.remotedesktop.client.capture.RobotScreenCaptureService;
import com.cleanroom.remotedesktop.client.capture.ScreenCaptureService;
import com.cleanroom.remotedesktop.client.clipboard.ClipboardSyncService;
import com.cleanroom.remotedesktop.client.file.FileTransferService;
import com.cleanroom.remotedesktop.client.input.InputInjectionService;
import com.cleanroom.remotedesktop.client.input.RobotInputInjectionService;
import com.cleanroom.remotedesktop.client.net.ClientNetworkManager;
import com.cleanroom.remotedesktop.protocol.Packet;
import com.cleanroom.remotedesktop.protocol.PacketType;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.*;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteKeyboardEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.SystemAction;
import com.cleanroom.remotedesktop.protocol.model.JsonPayloadUtil;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.*;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.*;
import com.cleanroom.remotedesktop.security.AuthCryptoHelper;
import com.cleanroom.remotedesktop.security.RandomPasswordGenerator;
import com.cleanroom.remotedesktop.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HostAgent {
    private static final Logger log = LoggerFactory.getLogger(HostAgent.class);

    private final ClientNetworkManager networkManager;
    private final RateLimiter rateLimiter = new RateLimiter(5, 60_000); // Max 5 failures / min
    private final FileTransferService fileService = new FileTransferService();

    private ScreenCaptureService captureService;
    private InputInjectionService inputService;
    private ClipboardSyncService clipboardService;

    private String hostId;
    private String randomPassword;
    private String customPassword;

    private volatile boolean inSession = false;
    private String activePartnerId;
    private String currentNonce;
    private ScheduledExecutorService captureExecutor;
    private long frameSequence = 0;

    // UI Callbacks
    private Consumer<String> onHostIdAssigned;
    private BiConsumer<String, Consumer<Boolean>> consentPromptHandler;
    private Consumer<Boolean> onSessionStateChanged;
    private Consumer<ChatMessage> onChatMessageReceived;

    public HostAgent(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        this.randomPassword = RandomPasswordGenerator.generatePassword(4);
        initServices();
        registerPacketHandlers();
    }

    private void initServices() {
        this.captureService = new RobotScreenCaptureService();
        this.inputService = new RobotInputInjectionService();
        this.clipboardService = new ClipboardSyncService(this::sendClipboardData);
    }

    private void registerPacketHandlers() {
        networkManager.addListener(PacketType.REGISTER_HOST_RESP, this::onRegisterHostResp);
        networkManager.addListener(PacketType.CONNECT_PARTNER_REQ, this::onConnectPartnerReq);
        networkManager.addListener(PacketType.AUTH_RESPONSE, this::onAuthResponse);
        networkManager.addListener(PacketType.SESSION_TERMINATE, this::onSessionTerminate);
        networkManager.addListener(PacketType.QUALITY_SETTINGS, this::onQualitySettings);
        networkManager.addListener(PacketType.MOUSE_EVENT, this::onMouseEvent);
        networkManager.addListener(PacketType.KEYBOARD_EVENT, this::onKeyboardEvent);
        networkManager.addListener(PacketType.SYSTEM_ACTION, this::onSystemAction);
        networkManager.addListener(PacketType.CLIPBOARD_DATA, this::onClipboardData);
        networkManager.addListener(PacketType.CHAT_MESSAGE, this::onChatMessage);
        networkManager.addListener(PacketType.FILE_LIST_REQ, this::onFileListReq);
        networkManager.addListener(PacketType.FILE_CHUNK, this::onFileChunk);
    }

    public void registerWithRelay(String preferredId) {
        RegisterHostReq req = new RegisterHostReq(preferredId);
        networkManager.sendPacket(Packet.create(PacketType.REGISTER_HOST_REQ, JsonPayloadUtil.serialize(req)));
    }

    private void onRegisterHostResp(Packet packet) {
        RegisterHostResp resp = JsonPayloadUtil.deserialize(packet.getPayload(), RegisterHostResp.class);
        if (resp.isSuccess()) {
            this.hostId = resp.getHostId();
            log.info("Host registered with Relay. Assigned Host ID: {}", hostId);
            if (onHostIdAssigned != null) {
                onHostIdAssigned.accept(hostId);
            }
        } else {
            log.warn("Host registration failed: {}", resp.getMessage());
        }
    }

    private void onConnectPartnerReq(Packet packet) {
        ConnectPartnerReq req = JsonPayloadUtil.deserialize(packet.getPayload(), ConnectPartnerReq.class);
        this.activePartnerId = req.getPartnerId();
        this.currentNonce = AuthCryptoHelper.generateRandomHex(16);

        log.info("Incoming connection request from Partner ID: {}. Issuing auth challenge...", activePartnerId);
        AuthChallenge challenge = new AuthChallenge("salt_uv", currentNonce);
        networkManager.sendPacket(Packet.create(PacketType.AUTH_CHALLENGE, JsonPayloadUtil.serialize(challenge)));
    }

    private void onAuthResponse(Packet packet) {
        AuthResponse resp = JsonPayloadUtil.deserialize(packet.getPayload(), AuthResponse.class);
        String partnerId = resp.getPartnerId();

        if (!rateLimiter.isAllowed(partnerId)) {
            log.warn("Rate limit exceeded for partner ID: {}", partnerId);
            SessionRejected rejected = new SessionRejected("Too many failed attempts. Please wait 1 minute.");
            networkManager.sendPacket(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
            return;
        }

        boolean validRandom = AuthCryptoHelper.verifyHmac(randomPassword, currentNonce, resp.getHmacProof());
        boolean validCustom = customPassword != null && !customPassword.isEmpty() &&
                AuthCryptoHelper.verifyHmac(customPassword, currentNonce, resp.getHmacProof());

        if (!validRandom && !validCustom) {
            log.warn("Invalid password submitted by partner ID: {}", partnerId);
            rateLimiter.recordFailure(partnerId);
            SessionRejected rejected = new SessionRejected("Wrong password.");
            networkManager.sendPacket(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
            return;
        }

        rateLimiter.reset(partnerId);
        log.info("Password authenticated for partner ID: {}. Triggering MANDATORY host consent prompt...", partnerId);

        // Mandatory Consent Prompt
        if (consentPromptHandler != null) {
            consentPromptHandler.accept(partnerId, allowed -> {
                if (Boolean.TRUE.equals(allowed)) {
                    acceptSession(partnerId);
                } else {
                    rejectSession("Host user rejected the remote control request.");
                }
            });
        } else {
            // Default safe action: If no consent UI is attached, reject!
            log.warn("No consent prompt handler registered; rejecting session by default for safety.");
            rejectSession("Host machine denied remote connection request.");
        }
    }

    private synchronized void acceptSession(String partnerId) {
        this.inSession = true;
        this.activePartnerId = partnerId;

        // Notify Viewer
        SessionAccepted accepted = new SessionAccepted(UUID.randomUUID().toString(), hostId, inputService.isViewOnly());
        networkManager.sendPacket(Packet.create(PacketType.SESSION_ACCEPTED, JsonPayloadUtil.serialize(accepted)));

        // Send Initial Screen Metadata
        ScreenMetadata metadata = captureService.getMetadata();
        networkManager.sendPacket(Packet.create(PacketType.SCREEN_METADATA, JsonPayloadUtil.serialize(metadata)));
        inputService.setScreenDimensions(metadata.getScreenWidth(), metadata.getScreenHeight());

        // Start Streaming
        startScreenStreaming();
        clipboardService.start();

        if (onSessionStateChanged != null) {
            onSessionStateChanged.accept(true);
        }
        log.info("Remote session accepted and active with Partner ID: {}", partnerId);
    }

    private void rejectSession(String reason) {
        log.info("Session rejected: {}", reason);
        SessionRejected rejected = new SessionRejected(reason);
        networkManager.sendPacket(Packet.create(PacketType.SESSION_REJECTED, JsonPayloadUtil.serialize(rejected)));
        terminateSessionInternal();
    }

    public synchronized void disconnectSession() {
        if (!inSession) return;
        log.info("Host terminating session with Partner ID: {}", activePartnerId);
        SessionTerminate term = new SessionTerminate("Host ended the session.");
        networkManager.sendPacket(Packet.create(PacketType.SESSION_TERMINATE, JsonPayloadUtil.serialize(term)));
        terminateSessionInternal();
    }

    private void onSessionTerminate(Packet packet) {
        log.info("Received SESSION_TERMINATE from peer.");
        terminateSessionInternal();
    }

    private synchronized void terminateSessionInternal() {
        inSession = false;
        activePartnerId = null;
        stopScreenStreaming();
        clipboardService.stop();

        if (onSessionStateChanged != null) {
            onSessionStateChanged.accept(false);
        }
    }

    private void startScreenStreaming() {
        stopScreenStreaming();
        captureExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Host-Screen-Streamer");
            t.setDaemon(true);
            return t;
        });

        // 25 FPS stream loop (every 40ms)
        captureExecutor.scheduleWithFixedDelay(this::captureAndStreamFrame, 0, 40, TimeUnit.MILLISECONDS);
    }

    private void stopScreenStreaming() {
        if (captureExecutor != null) {
            captureExecutor.shutdownNow();
            captureExecutor = null;
        }
    }

    private void captureAndStreamFrame() {
        if (!inSession) return;
        try {
            List<ScreenTile> dirtyTiles = captureService.captureDirtyTiles();
            if (!dirtyTiles.isEmpty()) {
                ScreenTileBatch batch = new ScreenTileBatch(frameSequence++, System.currentTimeMillis(), dirtyTiles);
                networkManager.sendPacket(Packet.create(PacketType.SCREEN_TILE_BATCH, JsonPayloadUtil.serialize(batch)));
            }
        } catch (Exception e) {
            log.warn("Error capturing/streaming frame: {}", e.getMessage());
        }
    }

    private void onQualitySettings(Packet packet) {
        QualitySettings settings = JsonPayloadUtil.deserialize(packet.getPayload(), QualitySettings.class);
        captureService.setQuality(settings.getJpegQuality());
        if (settings.getSelectedMonitor() != captureService.getMetadata().getMonitorIndex()) {
            captureService.setSelectedMonitor(settings.getSelectedMonitor());
            networkManager.sendPacket(Packet.create(PacketType.SCREEN_METADATA, JsonPayloadUtil.serialize(captureService.getMetadata())));
        }
    }

    private void onMouseEvent(Packet packet) {
        RemoteMouseEvent event = JsonPayloadUtil.deserialize(packet.getPayload(), RemoteMouseEvent.class);
        inputService.handleMouseEvent(event);
    }

    private void onKeyboardEvent(Packet packet) {
        RemoteKeyboardEvent event = JsonPayloadUtil.deserialize(packet.getPayload(), RemoteKeyboardEvent.class);
        inputService.handleKeyboardEvent(event);
    }

    private void onSystemAction(Packet packet) {
        SystemAction action = JsonPayloadUtil.deserialize(packet.getPayload(), SystemAction.class);
        inputService.handleSystemAction(action);
    }

    private void onClipboardData(Packet packet) {
        ClipboardData data = JsonPayloadUtil.deserialize(packet.getPayload(), ClipboardData.class);
        clipboardService.onRemoteClipboardReceived(data);
    }

    private void sendClipboardData(ClipboardData data) {
        if (inSession) {
            networkManager.sendPacket(Packet.create(PacketType.CLIPBOARD_DATA, JsonPayloadUtil.serialize(data)));
        }
    }

    private void onChatMessage(Packet packet) {
        ChatMessage msg = JsonPayloadUtil.deserialize(packet.getPayload(), ChatMessage.class);
        if (onChatMessageReceived != null) {
            onChatMessageReceived.accept(msg);
        }
    }

    public void sendChatMessage(String text) {
        if (inSession && text != null && !text.trim().isEmpty()) {
            ChatMessage msg = new ChatMessage(hostId, "Host", text.trim(), System.currentTimeMillis());
            networkManager.sendPacket(Packet.create(PacketType.CHAT_MESSAGE, JsonPayloadUtil.serialize(msg)));
        }
    }

    private void onFileListReq(Packet packet) {
        FileListReq req = JsonPayloadUtil.deserialize(packet.getPayload(), FileListReq.class);
        FileListResp resp = fileService.listFiles(req.getPath());
        networkManager.sendPacket(Packet.create(PacketType.FILE_LIST_RESP, JsonPayloadUtil.serialize(resp)));
    }

    private void onFileChunk(Packet packet) {
        FileChunk chunk = JsonPayloadUtil.deserialize(packet.getPayload(), FileChunk.class);
        boolean done = fileService.receiveChunk(chunk);
        FileAck ack = new FileAck(chunk.getTransferId(), chunk.getChunkIndex(), done ? FileAck.Status.OK : FileAck.Status.OK);
        networkManager.sendPacket(Packet.create(PacketType.FILE_ACK, JsonPayloadUtil.serialize(ack)));
    }

    // Getters and Setters
    public String getHostId() { return hostId; }
    public String getRandomPassword() { return randomPassword; }
    public void regenerateRandomPassword() {
        this.randomPassword = RandomPasswordGenerator.generatePassword(4);
    }
    public String getCustomPassword() { return customPassword; }
    public void setCustomPassword(String customPassword) { this.customPassword = customPassword; }
    public boolean isInSession() { return inSession; }
    public String getActivePartnerId() { return activePartnerId; }

    public void setOnHostIdAssigned(Consumer<String> onHostIdAssigned) { this.onHostIdAssigned = onHostIdAssigned; }
    public void setConsentPromptHandler(BiConsumer<String, Consumer<Boolean>> handler) { this.consentPromptHandler = handler; }
    public void setOnSessionStateChanged(Consumer<Boolean> listener) { this.onSessionStateChanged = listener; }
    public void setOnChatMessageReceived(Consumer<ChatMessage> listener) { this.onChatMessageReceived = listener; }
}
