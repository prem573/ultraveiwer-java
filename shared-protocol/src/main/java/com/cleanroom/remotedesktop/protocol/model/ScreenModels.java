package com.cleanroom.remotedesktop.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class ScreenModels {

    public static class ScreenMetadata {
        private int screenWidth;
        private int screenHeight;
        private int monitorIndex;
        private int monitorCount;
        private int tileWidth;
        private int tileHeight;

        public ScreenMetadata() {}

        public ScreenMetadata(int screenWidth, int screenHeight, int monitorIndex, int monitorCount, int tileWidth, int tileHeight) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.monitorIndex = monitorIndex;
            this.monitorCount = monitorCount;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
        }

        public int getScreenWidth() { return screenWidth; }
        public void setScreenWidth(int screenWidth) { this.screenWidth = screenWidth; }
        public int getScreenHeight() { return screenHeight; }
        public void setScreenHeight(int screenHeight) { this.screenHeight = screenHeight; }
        public int getMonitorIndex() { return monitorIndex; }
        public void setMonitorIndex(int monitorIndex) { this.monitorIndex = monitorIndex; }
        public int getMonitorCount() { return monitorCount; }
        public void setMonitorCount(int monitorCount) { this.monitorCount = monitorCount; }
        public int getTileWidth() { return tileWidth; }
        public void setTileWidth(int tileWidth) { this.tileWidth = tileWidth; }
        public int getTileHeight() { return tileHeight; }
        public void setTileHeight(int tileHeight) { this.tileHeight = tileHeight; }
    }

    public static class ScreenTile {
        private int x;
        private int y;
        private int width;
        private int height;
        private String format; // "JPEG" or "PNG"
        private byte[] data;

        public ScreenTile() {}

        public ScreenTile(int x, int y, int width, int height, String format, byte[] data) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.format = format;
            this.data = data;
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
    }

    public static class ScreenTileBatch {
        private long frameIndex;
        private long timestamp;
        private List<ScreenTile> tiles = new ArrayList<>();

        public ScreenTileBatch() {}

        public ScreenTileBatch(long frameIndex, long timestamp, List<ScreenTile> tiles) {
            this.frameIndex = frameIndex;
            this.timestamp = timestamp;
            this.tiles = tiles != null ? tiles : new ArrayList<>();
        }

        public long getFrameIndex() { return frameIndex; }
        public void setFrameIndex(long frameIndex) { this.frameIndex = frameIndex; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public List<ScreenTile> getTiles() { return tiles; }
        public void setTiles(List<ScreenTile> tiles) { this.tiles = tiles; }
    }

    public static class QualitySettings {
        public enum QualityMode {
            SPEED,
            QUALITY,
            AUTO,
            CUSTOM
        }

        private QualityMode mode = QualityMode.AUTO;
        private float jpegQuality = 0.65f;
        private int targetFps = 25;
        private int selectedMonitor = 0;

        public QualitySettings() {}

        public QualitySettings(QualityMode mode, float jpegQuality, int targetFps, int selectedMonitor) {
            this.mode = mode;
            this.jpegQuality = jpegQuality;
            this.targetFps = targetFps;
            this.selectedMonitor = selectedMonitor;
        }

        public QualityMode getMode() { return mode; }
        public void setMode(QualityMode mode) { this.mode = mode; }
        public float getJpegQuality() { return jpegQuality; }
        public void setJpegQuality(float jpegQuality) { this.jpegQuality = jpegQuality; }
        public int getTargetFps() { return targetFps; }
        public void setTargetFps(int targetFps) { this.targetFps = targetFps; }
        public int getSelectedMonitor() { return selectedMonitor; }
        public void setSelectedMonitor(int selectedMonitor) { this.selectedMonitor = selectedMonitor; }
    }
}

