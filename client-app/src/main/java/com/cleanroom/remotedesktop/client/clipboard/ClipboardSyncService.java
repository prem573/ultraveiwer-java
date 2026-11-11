package com.cleanroom.remotedesktop.client.clipboard;

import com.cleanroom.remotedesktop.protocol.model.FeatureModels.ClipboardData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClipboardSyncService {
    private static final Logger log = LoggerFactory.getLogger(ClipboardSyncService.class);

    private final Clipboard clipboard;
    private final Consumer<ClipboardData> outgoingCallback;
    private ScheduledExecutorService scheduler;
    private volatile boolean enabled = true;
    private String lastSentHash = "";
    private String lastReceivedHash = "";

    public ClipboardSyncService(Consumer<ClipboardData> outgoingCallback) {
        this.outgoingCallback = outgoingCallback;
        Clipboard sysClip = null;
        try {
            sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Exception e) {
            log.warn("System clipboard unavailable: {}", e.getMessage());
        }
        this.clipboard = sysClip;
    }

    public synchronized void start() {
        if (clipboard == null || scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Clipboard-Watcher");
            t.setDaemon(true);
            return t;
        });

        // Poll every 500ms for clipboard content change
        scheduler.scheduleWithFixedDelay(this::checkClipboard, 500, 500, TimeUnit.MILLISECONDS);
        log.info("Clipboard sync service started");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void checkClipboard() {
        if (!enabled || clipboard == null) return;

        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentText = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (currentText != null && !currentText.isEmpty()) {
                    String hash = String.valueOf(currentText.hashCode());
                    if (!Objects.equals(hash, lastSentHash) && !Objects.equals(hash, lastReceivedHash)) {
                        lastSentHash = hash;
                        if (outgoingCallback != null) {
                            outgoingCallback.accept(new ClipboardData(currentText, System.currentTimeMillis()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Clipboard may be temporarily locked by another process
        }
    }

    public void onRemoteClipboardReceived(ClipboardData data) {
        if (!enabled || clipboard == null || data == null || data.getText() == null) {
            return;
        }

        try {
            String text = data.getText();
            lastReceivedHash = String.valueOf(text.hashCode());
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, selection);
            log.debug("Updated local clipboard from remote peer");
        } catch (Exception e) {
            log.warn("Failed to set local clipboard: {}", e.getMessage());
        }
    }
}

