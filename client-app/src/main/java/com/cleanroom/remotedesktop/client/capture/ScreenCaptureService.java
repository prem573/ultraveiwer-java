package com.cleanroom.remotedesktop.client.capture;

import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenMetadata;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTile;

import java.util.List;

public interface ScreenCaptureService {
    ScreenMetadata getMetadata();
    List<ScreenTile> captureDirtyTiles();
    void setQuality(float jpegQuality);
    void setSelectedMonitor(int monitorIndex);
    int getMonitorCount();
    void close();
}
