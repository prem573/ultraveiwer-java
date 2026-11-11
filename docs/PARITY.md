# UltraViewer Behavioral & UI Parity Checklist

This checklist tracks the feature and behavioral parity between the clean-room Java remote desktop application and UltraViewer 6.6.x.

| Category | Observed UltraViewer Behavior | Clean-Room Java Implementation | Parity Status |
| :--- | :--- | :--- | :--- |
| **Main Window** | Dual-pane: "Allow Remote Control" (left) & "Control a Remote Computer" (right) | Dual-pane JavaFX layout with identical logical sections | [ ] Planned |
| **Main Window** | Numeric "Your ID" (9 digits formatted with spaces `xxx xxx xxx`) | Automatic 9-digit Host ID generated/allocated via relay | [ ] Planned |
| **Main Window** | 4-character random password with refresh/regenerate icon | Cryptographically secure random alphanumeric password generator + refresh button | [ ] Planned |
| **Main Window** | "Custom Password" configuration dialog | Custom unattended password configuration option | [ ] Planned |
| **Main Window** | "Connect to partner" button with status indicator ("Ready to connect") | Matching status label and connection progress indicator | [ ] Planned |
| **Safety & Consent** | Passive notification when connected | **Mandatory Prompt Dialog**: Host must explicitly accept or reject with countdown timer | [ ] Planned |
| **Safety & Consent** | Small tray/session indicator | **Persistent Overlay Banner**: Always on top at bottom-right with instant Disconnect button | [ ] Planned |
| **Safety & Consent** | Windows service persistent agent | Strict non-stealth user application; zero silent persistence | [ ] Planned |
| **Networking** | Central relay routing on port 443/2112 | Netty TLS relay server with numeric session routing and rate limiting | [ ] Planned |
| **Session Auth** | Challenge/response ID + Password check | Salted SHA-256 / TLS challenge handshake with max 5 retries / min | [ ] Planned |
| **Screen Streaming** | Tile/block capture with JPEG/PNG compression | Grid-based dirty-tile diff engine with parallel JPEG/PNG encoder | [ ] Planned |
| **Screen Controls** | Quality presets: Auto, Speed, Quality, Custom | Dynamic quality modes: Auto, High Speed (low JPEG), High Quality (lossless PNG) | [ ] Planned |
| **Screen Controls** | Screen scaling: Original 100%, Best scaled, Full screen | JavaFX Canvas with preserve-aspect-ratio letterboxing & Fullscreen toggle | [ ] Planned |
| **Multi-Monitor** | Switch active monitor: Monitor 1, Monitor 2, Show all | Monitor selector querying `GraphicsEnvironment` screen devices | [ ] Planned |
| **Remote Input** | Mouse movement, left/right/middle click, wheel scroll, drag | Viewer mouse listener mapped to Host `java.awt.Robot` events | [ ] Planned |
| **Remote Input** | Keyboard keydown, keyup, text typing | Viewer key event filter mapped to Host `Robot.keyPress`/`keyRelease` | [ ] Planned |
| **Remote Input** | View-Only Mode (Locks remote controls) | Dedicated toggle in Viewer toolbar to suppress input event dispatching | [ ] Planned |
| **Clipboard Sync** | Bidirectional text clipboard sharing with toggle | System clipboard monitoring & synchronized clipboard packet exchange | [ ] Planned |
| **In-Session Chat** | Floating chat drawer/window with message history and typing alerts | Integrated JavaFX chat window with instant bi-directional messaging | [ ] Planned |
| **File Transfer** | Dual-pane Explorer window (Local vs. Remote) with Upload/Download | Multi-pane file manager over chunked TLS protocol with progress bars | [ ] Planned |
| **Session Actions** | Send Ctrl+Alt+Del, Lock Screen, Disconnect | Remote action menu in viewer toolbar triggering platform actions | [ ] Planned |
| **Packaging** | Windows standalone installer / executable | Multi-platform Maven build + `jpackage` script for Windows exe/msi | [ ] Planned |
