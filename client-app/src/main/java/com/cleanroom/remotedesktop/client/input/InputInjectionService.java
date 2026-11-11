package com.cleanroom.remotedesktop.client.input;

import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteKeyboardEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.SystemAction;

public interface InputInjectionService {
    void handleMouseEvent(RemoteMouseEvent event);
    void handleKeyboardEvent(RemoteKeyboardEvent event);
    void handleSystemAction(SystemAction action);
    void setViewOnly(boolean viewOnly);
    boolean isViewOnly();
    void setScreenDimensions(int width, int height);
}
