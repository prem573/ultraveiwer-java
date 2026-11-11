# Specification: Java Remote Desktop (Clean-Room Implementation)

## 1. Overview & Objective
This specification defines the functional, behavioral, architectural, and safety requirements for a clean-room Java remote desktop application inspired by the workflow of UltraViewer 6.6.x. The implementation utilizes original protocols, original UI components, original assets, and its own relay server architecture.

---

## 2. Evidence Basis & Source Analysis

### 2.1 Artifacts Examined
- **Primary Binary**: `C:\Program Files (x86)\UltraViewer\UltraViewer_Desktop.exe` (PE32 x86, SHA-256: `fe0e61de04c0b827fcf6e041158e443f77439d257ced4e21a6d16bdd3d52d55b`)
- **Primary Engine Assembly**: `RemoteControl.dll` (.NET Framework 4.0 CLI PE32, SHA-256: `43b76e2c133323e6cbbd30843f3d98307aaa19ee5ca682729d923c807833117a`)
- **Supporting Native Modules**: `uvc.dll` (video capture/codec), `uvh.dll` / `uvh64.dll` (input hooks), `uva.dll` / `uva64.dll` (audio capture), `uv_x64.exe` (64-bit helper process), `UltraViewer_Service.exe` (Session 0 / Winlogon service).
- **Configuration & Localization Data**: `Language/English.txt`, `UltraViewerService_log.txt`, `ConnectionLog.log`.

### 2.2 Clean-Room Boundary
- No decompiled source code, proprietary algorithms, encryption keys, or proprietary packet serialization structures are copied.
- All wire protocols, message formats, and server architectures are independently designed and specified in `docs/DESIGN.md`.
- All icons, layouts, and graphical elements are created independently.

---

## 3. High-Level Architectural Roles

The system comprises three primary functional roles:

```
+-----------------------------------------------------------+
|                       Relay Server                         |
|   - Session Routing by Numeric ID                         |
|   - TLS Encrypted Relaying & Ephemeral Handshake          |
|   - Rate Limiting & Access Control                        |
+-----------------------------------------------------------+
               ^                             ^
               | TLS Tunnel                  | TLS Tunnel
               v                             v
+-----------------------------+   +-----------------------------+
|         Host Agent          |   |        Viewer Client        |
| - Screen Capture (Robot/JNA)|   | - Canvas Rendering Engine   |
| - Tile Diff & JPEG Encoder  |   | - Input Event Forwarder     |
| - Input Injection (Robot)   |   | - In-Session Toolbar & Chat |
| - Mandatory Consent Dialog  |   | - File Transfer Client      |
| - Persistent Status Banner  |   |                             |
+-----------------------------+   +-----------------------------+
```

1. **Relay Server**:
   - Acts as the rendezvous point and traffic forwarder for NAT traversal.
   - Manages client registrations, mapping numeric partner IDs (e.g. 9-digit integers) to active TLS connections.
   - Forwards screen frames, input events, chat messages, and file chunks between paired peers.
2. **Host Agent (Controlled Machine)**:
   - Registers with the Relay Server and obtains an assigned 9-digit Host ID.
   - Generates a cryptographically random session password.
   - Listens for incoming connection requests forwarded by the relay.
   - Enforces mandatory user consent prompt and persistent on-screen status banner.
   - Captures screen frames, computes dirty rectangle/tile diffs, compresses images, and streams them to the viewer.
   - Receives sanitized mouse/keyboard input events from the viewer and injects them locally.
3. **Viewer Client (Controlling Machine)**:
   - Connects to the Relay Server and requests a session with the specified Partner ID.
   - Authenticates using the host's session password.
   - Displays the remote screen in an interactive JavaFX canvas.
   - Translates local mouse clicks, movements, scrolls, and key presses into protocol events.
   - Provides controls for display scaling, quality, monitor switching, clipboard sync, file transfer, and chat.

---

## 4. Feature Requirements

### 4.1 Identification & Session Authentication
- **Host ID**: A stable or session-assigned 9-digit numeric identifier (e.g., `135 210 707`) uniquely identifying the host on the relay.
- **Random Password**: A 4-to-6 alphanumeric character password generated randomly on startup, with an on-demand "Regenerate" button.
- **Custom Password**: Optional user-configured static password for unattended access.
- **Authentication**: Zero-knowledge or salted hashed password verification over TLS; strict rate limiting (max 5 failed attempts per minute) to prevent brute-force attacks.

### 4.2 Mandatory Safety & Consent Architecture
- **Mandatory Host Consent Dialog**:
  - Whenever an incoming connection attempt passes authentication, the host system displays a modal prompt:
    `"Partner [ID] is requesting remote control. Do you want to allow this session?"`
  - The dialog includes an accept button, reject button, and a 30-second countdown timer that defaults to reject upon timeout.
- **Always-Visible "Being Controlled" Banner**:
  - A non-minimizable, always-on-top overlay window pinned to the bottom-right corner of the host desktop.
  - Displays: Partner ID, connection status, session duration timer, and an immediate **"Disconnect Now"** button.
- **Instant Termination**:
  - Clicking "Disconnect" on either host banner or viewer immediately severs the socket and ceases screen capture and input injection.
- **No Silent / Stealth Mode**:
  - No background capture without visual notification; no auto-start without explicit user opt-in.

### 4.3 Screen Capture & Video Transmission
- **Capture Pipeline**:
  - Screen grab via `java.awt.Robot.createScreenCapture` (with interface for hardware-accelerated/JNA capture).
  - Multi-monitor support: Query `GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()` to select Monitor 1, Monitor 2, or Virtual Desktop (all monitors).
- **Encoding & Streaming**:
  - Grid-based tile decomposition (e.g., 64x64 or 128x128 pixel tiles).
  - Dirty rectangle detection (hashing or pixel delta) so only modified tiles are compressed and transmitted.
  - Configurable JPEG/PNG compression quality:
    - *Optimize Speed*: Lower JPEG quality (~40-60%), reduced color depth, higher framerate target (up to 30 FPS).
    - *Optimize Quality*: High JPEG quality (~80-90%) or lossless PNG for static text, 15-20 FPS.
    - *Auto*: Dynamically adapts tile quality based on measured round-trip time and socket buffer backpressure.

### 4.4 Remote Control & View-Only Mode
- **Remote Control Mode**: Viewer mouse events (move, press, release, wheel) and keyboard events (typed, press, release) are serialized and injected on the host via `java.awt.Robot`.
- **View-Only Mode**: A toggleable mode where input injection is disabled on the host; the viewer only receives screen frames.
- **System Actions**: In-session action to send `Ctrl+Alt+Del` (or lock screen request) and clipboard sync.

### 4.5 Clipboard Synchronization
- Text clipboard listener detects local changes and transmits delta to the peer when enabled.
- Option to toggle clipboard sharing: Bidirectional, Host-to-Viewer only, Viewer-to-Host only, or Disabled.

### 4.6 In-Session Chat
- Bi-directional text messaging panel available during active sessions.
- Notification badge / popup when a new message arrives while chat window is closed.
- Quick phrase suggestions or saved snippets.

### 4.7 File Transfer
- Dedicated dual-pane file transfer manager:
  - Left: Local filesystem explorer.
  - Right: Remote filesystem explorer.
- Actions: Navigate directories, Upload file/folder, Download file/folder, Cancel transfer.
- Chunked transfer over TLS stream with SHA-256 chunk validation and transfer progress tracking.

---

## 5. User Interface Layout & Workflows

### 5.1 Main Window (Dual-Pane Layout)
```
+--------------------------------------------------------------------------+
|  File   Settings   Help                                           -  [X] |
+--------------------------------------------------------------------------+
|                                                                          |
|   ALLOW REMOTE CONTROL                    CONTROL A REMOTE COMPUTER      |
|   --------------------                    -------------------------      |
|   Please tell your partner the            Enter partner's ID to connect  |
|   following ID and password:              to their computer:             |
|                                                                          |
|   Your ID:                                Partner ID:                    |
|   [  135 210 707      ] [Copy]            [                       ]      |
|                                                                          |
|   Password:                               Password:                      |
|   [  a 9 f 2          ] [Refresh]         [                       ]      |
|                                                                          |
|   [x] Set custom password                 [  Connect to partner   ]      |
|                                                                          |
+--------------------------------------------------------------------------+
|  Status: Ready to connect                                                |
+--------------------------------------------------------------------------+
```

### 5.2 In-Session Viewer Window
- **Top Toolbar**:
  - `[Actions]` (Send Ctrl+Alt+Del, Lock Screen, Disconnect)
  - `[Display]` (Quality: Speed / Quality / Auto; Scaling: 100% / Best Fit / Fullscreen)
  - `[Screen]` (Select Monitor 1 / Monitor 2 / All)
  - `[View Only]` (Toggle control lock)
  - `[Clipboard]` (Toggle clipboard sync)
  - `[Chat]` (Toggle chat dock/window)
  - `[File Transfer]` (Open file manager dialog)
- **Viewport Canvas**: Resizable display rendering decoded screen frames with accurate coordinate scaling.

### 5.3 Host Overlay Window
- Docked at bottom-right corner of host screen, non-minimizable.
- Displays: `"Remote session active with Partner: [Partner ID]"`
- Buttons: `[Disconnect]` (prominent red button), `[Chat]`.

---

## 6. Known Unknowns & System Limitations
1. **Low-Level Windows UAC / Secure Desktop Injection**: Pure Java (`java.awt.Robot`) runs in user session and cannot inject input into Windows Secure Desktop (e.g. UAC prompts or Ctrl+Alt+Del winlogon screen) without a Windows Service or elevated UIPI bypass helper. This is documented as a known design boundary; our core implementation will focus on user-session remote control with an upgrade path to a native helper process.
2. **Direct P2P UDP Hole Punching vs. Relay**: UltraViewer primarily routes through central relay servers (ports 443 / 2112) with optional direct TCP fallback. Our implementation will deploy a Netty-based relay server, which guarantees traversal through all firewalls and NATs.
