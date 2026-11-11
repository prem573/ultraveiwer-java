package com.cleanroom.remotedesktop.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class FeatureModels {

    public static class ChatMessage {
        private String senderId;
        private String senderName;
        private String text;
        private long timestamp;

        public ChatMessage() {}

        public ChatMessage(String senderId, String senderName, String text, long timestamp) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.text = text;
            this.timestamp = timestamp;
        }

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class ClipboardData {
        private String text;
        private long timestamp;

        public ClipboardData() {}

        public ClipboardData(String text, long timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class FileItem {
        private String name;
        private boolean directory;
        private long size;
        private long lastModified;

        public FileItem() {}

        public FileItem(String name, boolean directory, long size, long lastModified) {
            this.name = name;
            this.directory = directory;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isDirectory() { return directory; }
        public void setDirectory(boolean directory) { this.directory = directory; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }

    public static class FileListReq {
        private String path;

        public FileListReq() {}
        public FileListReq(String path) { this.path = path; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class FileListResp {
        private String path;
        private List<FileItem> items = new ArrayList<>();

        public FileListResp() {}

        public FileListResp(String path, List<FileItem> items) {
            this.path = path;
            this.items = items != null ? items : new ArrayList<>();
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public List<FileItem> getItems() { return items; }
        public void setItems(List<FileItem> items) { this.items = items; }
    }

    public static class FileChunk {
        private String transferId;
        private String targetPath;
        private int chunkIndex;
        private int totalChunks;
        private byte[] data;
        private String sha256;

        public FileChunk() {}

        public FileChunk(String transferId, String targetPath, int chunkIndex, int totalChunks, byte[] data, String sha256) {
            this.transferId = transferId;
            this.targetPath = targetPath;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
            this.sha256 = sha256;
        }

        public String getTransferId() { return transferId; }
        public void setTransferId(String transferId) { this.transferId = transferId; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public String getSha256() { return sha256; }
        public void setSha256(String sha256) { this.sha256 = sha256; }
    }

    public static class FileAck {
        public enum Status {
            OK,
            RETRY,
            CANCELLED
        }

        private String transferId;
        private int chunkIndex;
        private Status status;

        public FileAck() {}

        public FileAck(String transferId, int chunkIndex, Status status) {
            this.transferId = transferId;
            this.chunkIndex = chunkIndex;
            this.status = status;
        }

        public String getTransferId() { return transferId; }
        public void setTransferId(String transferId) { this.transferId = transferId; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
    }

    public static class HeartbeatPing {
        private long timestamp;
        public HeartbeatPing() { this.timestamp = System.currentTimeMillis(); }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class HeartbeatPong {
        private long pingTimestamp;
        private long pongTimestamp;

        public HeartbeatPong() {}
        public HeartbeatPong(long pingTimestamp) {
            this.pingTimestamp = pingTimestamp;
            this.pongTimestamp = System.currentTimeMillis();
        }

        public long getPingTimestamp() { return pingTimestamp; }
        public void setPingTimestamp(long pingTimestamp) { this.pingTimestamp = pingTimestamp; }
        public long getPongTimestamp() { return pongTimestamp; }
        public void setPongTimestamp(long pongTimestamp) { this.pongTimestamp = pongTimestamp; }
    }
}
