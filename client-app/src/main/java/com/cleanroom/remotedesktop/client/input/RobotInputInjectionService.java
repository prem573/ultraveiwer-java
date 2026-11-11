package com.cleanroom.remotedesktop.client.input;

import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteKeyboardEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.InputEvent;

public class RobotInputInjectionService implements InputInjectionService {
    private static final Logger log = LoggerFactory.getLogger(RobotInputInjectionService.class);

    private final Robot robot;
    private volatile boolean viewOnly = false;
    private int screenWidth = 1920;
    private int screenHeight = 1080;

    public RobotInputInjectionService() {
        try {
            this.robot = new Robot();
            this.robot.setAutoWaitForIdle(false);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenWidth = screenSize.width;
            this.screenHeight = screenSize.height;
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize Robot for input injection", e);
        }
    }

    @Override
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width > 0 ? width : 1920;
        this.screenHeight = height > 0 ? height : 1080;
    }

    @Override
    public void setViewOnly(boolean viewOnly) {
        this.viewOnly = viewOnly;
        log.info("View-Only mode set to: {}", viewOnly);
    }

    @Override
    public boolean isViewOnly() {
        return viewOnly;
    }

    @Override
    public void handleMouseEvent(RemoteMouseEvent event) {
        if (viewOnly || robot == null || event == null) {
            return;
        }

        try {
            int absX = (int) Math.round(event.getxPercent() * screenWidth);
            int absY = (int) Math.round(event.getyPercent() * screenHeight);

            switch (event.getAction()) {
                case MOVE -> robot.mouseMove(absX, absY);
                case PRESS -> {
                    robot.mouseMove(absX, absY);
                    int mask = getButtonMask(event.getButton());
                    if (mask != 0) robot.mousePress(mask);
                }
                case RELEASE -> {
                    robot.mouseMove(absX, absY);
                    int mask = getButtonMask(event.getButton());
                    if (mask != 0) robot.mouseRelease(mask);
                }
                case WHEEL -> {
                    if (event.getWheelDelta() != 0) {
                        robot.mouseWheel(event.getWheelDelta());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error injecting mouse event: {}", e.getMessage());
        }
    }

    private int getButtonMask(RemoteMouseEvent.Button button) {
        return switch (button) {
            case LEFT -> InputEvent.BUTTON1_DOWN_MASK;
            case MIDDLE -> InputEvent.BUTTON2_DOWN_MASK;
            case RIGHT -> InputEvent.BUTTON3_DOWN_MASK;
            default -> 0;
        };
    }

    @Override
    public void handleKeyboardEvent(RemoteKeyboardEvent event) {
        if (viewOnly || robot == null || event == null) {
            return;
        }

        try {
            int keyCode = event.getKeyCode();
            if (keyCode <= 0) return;

            switch (event.getAction()) {
                case PRESS -> robot.keyPress(keyCode);
                case RELEASE -> robot.keyRelease(keyCode);
                case TYPED -> {} // Handled by PRESS/RELEASE
            }
        } catch (IllegalArgumentException e) {
            log.debug("Unrecognized keyCode for injection: {}", event.getKeyCode());
        } catch (Exception e) {
            log.warn("Error injecting keyboard event: {}", e.getMessage());
        }
    }

    @Override
    public void handleSystemAction(SystemAction action) {
        if (viewOnly || action == null) return;

        switch (action.getActionType()) {
            case LOCK_SCREEN -> {
                log.info("System action requested: LOCK_SCREEN");
                try {
                    // Windows lock workstation
                    Runtime.getRuntime().exec(new String[]{"rundll32.exe", "user32.dll,LockWorkStation"});
                } catch (Exception e) {
                    log.error("Failed to execute LockWorkStation", e);
                }
            }
            case CTRL_ALT_DEL -> {
                log.info("System action requested: CTRL_ALT_DEL (Requires Windows Secure Service)");
            }
            case SWITCH_SIDE -> {
                log.info("System action requested: SWITCH_SIDE");
            }
        }
    }
}
