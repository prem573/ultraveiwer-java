package com.cleanroom.remotedesktop.protocol.model;

public class InputModels {

    public static class RemoteMouseEvent {
        public enum Action {
            MOVE,
            PRESS,
            RELEASE,
            WHEEL
        }

        public enum Button {
            NONE,
            LEFT,
            MIDDLE,
            RIGHT
        }

        private Action action;
        private double xPercent; // 0.0 to 1.0
        private double yPercent; // 0.0 to 1.0
        private Button button = Button.NONE;
        private int wheelDelta = 0;

        public RemoteMouseEvent() {}

        public RemoteMouseEvent(Action action, double xPercent, double yPercent, Button button, int wheelDelta) {
            this.action = action;
            this.xPercent = xPercent;
            this.yPercent = yPercent;
            this.button = button;
            this.wheelDelta = wheelDelta;
        }

        public Action getAction() { return action; }
        public void setAction(Action action) { this.action = action; }
        public double getxPercent() { return xPercent; }
        public void setxPercent(double xPercent) { this.xPercent = xPercent; }
        public double getyPercent() { return yPercent; }
        public void setyPercent(double yPercent) { this.yPercent = yPercent; }
        public Button getButton() { return button; }
        public void setButton(Button button) { this.button = button; }
        public int getWheelDelta() { return wheelDelta; }
        public void setWheelDelta(int wheelDelta) { this.wheelDelta = wheelDelta; }
    }

    public static class RemoteKeyboardEvent {
        public enum Action {
            PRESS,
            RELEASE,
            TYPED
        }

        private Action action;
        private int keyCode; // Standard java.awt.event.KeyEvent code
        private char keyChar;
        private int modifiers;

        public RemoteKeyboardEvent() {}

        public RemoteKeyboardEvent(Action action, int keyCode, char keyChar, int modifiers) {
            this.action = action;
            this.keyCode = keyCode;
            this.keyChar = keyChar;
            this.modifiers = modifiers;
        }

        public Action getAction() { return action; }
        public void setAction(Action action) { this.action = action; }
        public int getKeyCode() { return keyCode; }
        public void setKeyCode(int keyCode) { this.keyCode = keyCode; }
        public char getKeyChar() { return keyChar; }
        public void setKeyChar(char keyChar) { this.keyChar = keyChar; }
        public int getModifiers() { return modifiers; }
        public void setModifiers(int modifiers) { this.modifiers = modifiers; }
    }

    public static class SystemAction {
        public enum ActionType {
            CTRL_ALT_DEL,
            LOCK_SCREEN,
            SWITCH_SIDE
        }

        private ActionType actionType;

        public SystemAction() {}
        public SystemAction(ActionType actionType) { this.actionType = actionType; }

        public ActionType getActionType() { return actionType; }
        public void setActionType(ActionType actionType) { this.actionType = actionType; }
    }
}
