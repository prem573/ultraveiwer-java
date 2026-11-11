package com.cleanroom.remotedesktop.client;

import com.cleanroom.remotedesktop.client.host.HostAgent;
import com.cleanroom.remotedesktop.client.net.ClientNetworkManager;
import com.cleanroom.remotedesktop.client.viewer.ViewerSession;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.ChatMessage;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTileBatch;
import com.cleanroom.remotedesktop.protocol.model.SessionModels.SessionAccepted;
import com.cleanroom.remotedesktop.relay.RelayServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndSessionTest {
    private RelayServer relayServer;
    private int relayPort;
    private ClientNetworkManager hostNet;
    private ClientNetworkManager viewerNet;

    @BeforeEach
    public void setUp() throws Exception {
        // Start Relay with TLS on dynamic port
        relayServer = new RelayServer("127.0.0.1", 0, true);
        relayServer.start();
        relayPort = relayServer.getBoundPort();
    }

    @AfterEach
    public void tearDown() {
        if (viewerNet != null) viewerNet.disconnect();
        if (hostNet != null) hostNet.disconnect();
        if (relayServer != null) relayServer.stop();
    }

    @Test
    public void testCompleteEndToEndRemoteSessionFlow() throws Exception {
        // 1. Setup Host
        hostNet = new ClientNetworkManager("127.0.0.1", relayPort, true);
        hostNet.connect();
        HostAgent hostAgent = new HostAgent(hostNet);

        BlockingQueue<String> hostIdQueue = new LinkedBlockingQueue<>();
        hostAgent.setOnHostIdAssigned(hostIdQueue::offer);

        // Mandatory consent callback: simulate user clicking "Allow"
        AtomicBoolean consentPromptTriggered = new AtomicBoolean(false);
        hostAgent.setConsentPromptHandler((partnerId, callback) -> {
            consentPromptTriggered.set(true);
            callback.accept(true); // User allowed
        });

        // Track incoming chat on Host
        BlockingQueue<ChatMessage> hostChatQueue = new LinkedBlockingQueue<>();
        hostAgent.setOnChatMessageReceived(hostChatQueue::offer);

        // Register Host with Relay
        hostAgent.registerWithRelay("987654321");
        String assignedHostId = hostIdQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(assignedHostId, "Host should receive assigned Host ID");
        assertEquals("987654321", assignedHostId);

        String hostPassword = hostAgent.getRandomPassword();
        assertNotNull(hostPassword);

        // 2. Setup Viewer
        viewerNet = new ClientNetworkManager("127.0.0.1", relayPort, true);
        viewerNet.connect();
        ViewerSession viewerSession = new ViewerSession(viewerNet);

        BlockingQueue<SessionAccepted> sessionAcceptedQueue = new LinkedBlockingQueue<>();
        viewerSession.setOnSessionAccepted(sessionAcceptedQueue::offer);

        BlockingQueue<ScreenTileBatch> tileQueue = new LinkedBlockingQueue<>();
        viewerSession.setOnScreenTilesReceived(tileQueue::offer);

        // 3. Viewer connects to Host
        viewerSession.connectToPartner(assignedHostId, hostPassword);

        // 4. Verify Host consent prompt was triggered
        SessionAccepted accepted = sessionAcceptedQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(accepted, "Viewer should receive SessionAccepted");
        assertTrue(consentPromptTriggered.get(), "Host consent prompt MUST be triggered");
        assertTrue(hostAgent.isInSession(), "Host should be in active session");
        assertTrue(viewerSession.isConnected(), "Viewer should be connected");

        // 5. Verify screen tiles are streamed to Viewer
        ScreenTileBatch batch = tileQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(batch, "Viewer should receive at least one screen tile batch");
        assertNotNull(batch.getTiles());
        assertFalse(batch.getTiles().isEmpty());

        // 6. Test Input Injection & Chat
        viewerSession.sendMouseEvent(new RemoteMouseEvent(
                RemoteMouseEvent.Action.MOVE, 0.5, 0.5, RemoteMouseEvent.Button.NONE, 0
        ));

        viewerSession.sendChatMessage("Hello from test viewer!");
        ChatMessage receivedChat = hostChatQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedChat, "Host should receive viewer chat message");
        assertEquals("Hello from test viewer!", receivedChat.getText());

        // 7. Viewer disconnects
        viewerSession.disconnect();
        Thread.sleep(200);

        assertFalse(hostAgent.isInSession(), "Host session should be terminated after disconnect");
    }
}

