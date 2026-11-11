# UltraViewer Behavioral & UI Parity Checklist

This checklist tracks the feature and behavioral parity between the clean-room Java remote desktop application and UltraViewer 6.6.x.

| Category | Observed UltraViewer Behavior | Clean-Room Java Implementation | Parity Status |
| :--- | :--- | :--- | :--- |
| **Main Window** | Dual-pane: "Allow Remote Control" (left) & "Control a Remote Computer" (right) | Dual-pane JavaFX layout in `MainWindow.java` with matching sections and styling | [x] Completed |
| **Main Window** | Numeric "Your ID" (9 digits formatted with spaces `xxx xxx xxx`) | Automatic 9-digit Host ID generated via `RandomPasswordGenerator` + copy button | [x] Completed |
| **Main Window** | 4-character random password with refresh/regenerate icon | Cryptographically secure random 4-char password generator + refresh button | [x] Completed |
| **Main Window** | "Custom Password" configuration dialog | Custom unattended password configuration option in `SettingsDialog.java` | [x] Completed |
| **Main Window** | "Connect to partner" button with status indicator ("Ready to connect") | Status bar with colored indicator dot and active Host ID status | [x] Completed |
| **Safety & Consent** | Passive notification when connected | **Mandatory Prompt Dialog**: Host modal dialog (`HostConsentDialog.java`) with 30s auto-reject timer | [x] Completed |
| **Safety & Consent** | Small tray/session indicator | **Persistent Overlay Banner**: Always on top at bottom-right (`HostFloatingBanner.java`) with instant Disconnect | [x] Completed |
| **Safety & Consent** | Windows service persistent agent | Strict non-stealth user application; zero silent persistence; instant termination | [x] Completed |
| **Networking** | Central relay routing on port 443/2112 | Netty TLS 1.3 relay server (`RelayServer.java`) with numeric session routing and rate limiting | [x] Completed |
| **Session Auth** | Challenge/response ID + Password check | Salted HMAC-SHA256 challenge/response handshake with sliding-window `RateLimiter` | [x] Completed |
| **Screen Streaming** | Tile/block capture with JPEG/PNG compression | Grid-based dirty-tile diff engine (`RobotScreenCaptureService.java`) with parallel JPEG encoder | [x] Completed |
| **Screen Controls** | Quality presets: Auto, Speed, Quality, Custom | Dynamic quality modes: Auto (0.65), Speed (0.40, 30 FPS), Quality (0.85, 15 FPS) | [x] Completed |
| **Screen Controls** | Screen scaling: Original 100%, Best scaled, Full screen | Canvas rendering with aspect-ratio letterboxing, Best Scaled, and Full Screen | [x] Completed |
| **Multi-Monitor** | Switch active monitor: Monitor 1, Monitor 2, Show all | Query `GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()` | [x] Completed |
| **Remote Input** | Mouse movement, left/right/middle click, wheel scroll, drag | Viewer mouse listeners mapped to Host `Robot.mouseMove` and `mousePress`/`mouseRelease` | [x] Completed |
| **Remote Input** | Keyboard keydown, keyup, text typing | Viewer key filter mapped to Host `Robot.keyPress`/`keyRelease` | [x] Completed |
| **Remote Input** | View-Only Mode (Locks remote controls) | Dedicated toggle in toolbar (`viewOnly`) to suppress input event dispatching | [x] Completed |
| **Clipboard Sync** | Bidirectional text clipboard sharing with toggle | System clipboard monitoring (`ClipboardSyncService.java`) with loop-guard hashing | [x] Completed |
| **In-Session Chat** | Floating chat drawer/window with message history and typing alerts | Integrated chat drawer in `ViewerWindow.java` and host chat window with timestamps | [x] Completed |
| **File Transfer** | Dual-pane Explorer window (Local vs. Remote) with Upload/Download | Multi-pane file manager (`FileTransferDialog.java`) over chunked TLS protocol | [x] Completed |
| **Session Actions** | Send Ctrl+Alt+Del, Lock Screen, Disconnect | Remote action menu in viewer toolbar triggering platform actions | [x] Completed |
| **Packaging** | Windows standalone installer / executable | Standalone Windows `.exe` packaged via `jpackage` (`CleanRoomRemoteDesktop.exe`) | [x] Completed |
