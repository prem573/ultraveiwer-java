# Clean-Room Java Remote Desktop

A high-performance, clean-room implementation of a desktop remote-control application inspired by the **UltraViewer** workflow, built from scratch with **Java 21**, **Netty**, **JavaFX**, and **TLS 1.3**.

---

## 🚀 Key Features

* **Instant ID & Password Sessions**: 9-digit Partner ID pairing with random 4-character session passwords or optional static unattended passwords.
* **Direct Screen Connection**: Instant remote control upon entering partner ID and password.
* **Dirty-Tile Screen Capture**: 128×128 CRC32 dirty-tile diffing with multi-threaded JPEG compression (up to 95% Ultra HD quality) for smooth framerates and minimal network bandwidth.
* **Full Remote Control**: Mouse tracking, clicks, dragging, scrolling, and keyboard keystrokes mapped with precision.
* **File Transfer**: Dedicated two-way chunked file transfer window to upload and download files between machines.
* **Real-Time In-Session Chat**: Embedded messaging drawer for text communication during support sessions.
* **Clipboard Synchronization**: Automatic bidirectional text clipboard sync between host and viewer.
* **Multi-Monitor Support**: Switch between connected screens on multi-monitor systems on the fly.
* **Display & Scaling Controls**: Auto-fit scaling, 1:1 original resolution, full-screen mode (`F11`), and real-time quality presets (Ultra HD, High, Balanced, Low Bandwidth).
* **Enterprise Security**:
  * Mandatory TLS 1.3 transport encryption.
  * Salted HMAC-SHA256 challenge-response authentication (passwords never sent in plain text).
  * In-memory brute-force rate limiter (auto-blocks repeated incorrect passwords).
  * Always-accessible host-side **Disconnect Partner** button for instant session termination.

---

## 🏛 Architecture & Modules

The project is structured as a clean Maven multi-module repository:

```
ultraveiwer-java/
├── shared-protocol/     # Custom binary frame codec, packet DTOs, HMAC crypto, TLS factory
├── relay-server/        # High-throughput headless Netty server routing sessions by 9-digit ID
├── client-app/          # Unified JavaFX desktop app (contains both Host Agent & Viewer UI)
├── docs/                # Clean-room documentation (SPEC.md, DESIGN.md, PARITY.md)
└── package-jars.ps1     # One-click packaging script for standalone runnable JARs
```

### Wire Protocol Framing
Every network packet is framed with a standard 8-byte header over TLS 1.3:
```
+---------------+---------------+-------------------+--------------------+
| Magic (2B)    | Type (2B)     | Sequence (2B)     | Payload Len (2B)   |
| 0xCAFE        | PacketType ID | Monotonic counter | Byte length (uint) |
+---------------+---------------+-------------------+--------------------+
|                           Payload Bytes (N bytes)                      |
+------------------------------------------------------------------------+
```

---

## 📋 Prerequisites

* **Runtime**: [Java 21 JRE or JDK](https://adoptium.net/temurin/releases/?version=21) *(Eclipse Temurin OpenJDK 21 x64 recommended)*.
* **Build tool**: [Apache Maven 3.9+](https://maven.apache.org/) (only needed if building from source).

---

## 🛠 Building from Source

To compile the entire multi-module project and generate standalone executable JARs:

### On Windows (PowerShell):
```powershell
.\package-jars.ps1
```

### On Linux / macOS:
```bash
mvn clean package -DskipTests
```

The compiled standalone executable JARs will be generated in `release/`:
* `release/remote-desktop-client.jar` (~25 MB fat JAR with JavaFX & Netty bundled)
* `release/remote-desktop-relay.jar` (~15 MB fat JAR)

---

## 💻 How to Run

### 1. Run the Relay Server (Cloud or Local VM)
On your cloud server (e.g. Ubuntu / Debian):
```bash
java -jar release/remote-desktop-relay.jar --port 2112
```

*(Optional) Run as a systemd background service on Linux:*
```ini
[Unit]
Description=Clean-Room Remote Desktop Relay Service
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/bin/java -jar /opt/remotedesktop/remote-desktop-relay.jar --port 2112
Restart=always

[Install]
WantedBy=multi-user.target
```

### 2. Run the Client Application
Launch the client JAR on any Windows, macOS, or Linux machine:
```bash
java -jar release/remote-desktop-client.jar
```

1. **Host Screen (Left Panel)**: Displays **Your ID** (e.g. `135 210 707`) and dynamic 4-character **Password**.
2. **Control Remote Partner (Right Panel)**: Enter the partner's 9-digit ID and password, then click **Connect to partner**.
3. **Settings**: Go to **Settings** $\rightarrow$ **Options / Configuration** to customize relay server IP, port, TLS settings, or unattended access passwords.

---

## 🧪 Running Automated Tests

Run the full automated test suite (including Netty loopback integration tests, encryption checks, and rate-limiting validation):

```bash
mvn test
```

---

## 📄 Clean-Room Compliance & Notice

This project was developed strictly following **clean-room software engineering** principles:
* No decompiled code, disassembly, private keys, or reverse-engineered binaries from proprietary vendors are included or used.
* All protocols, models, packet framing, network codecs, and user interface implementations are 100% original.
* Full functional specification, architecture design, and feature parity documentation are available under the [`docs/`](docs/) directory:
  * [`docs/SPEC.md`](docs/SPEC.md): Functional behavioral specification.
  * [`docs/DESIGN.md`](docs/DESIGN.md): Custom architecture, TLS framing, and security models.
  * [`docs/PARITY.md`](docs/PARITY.md): UI and workflow parity checklist.

---

## 📜 License

This project is licensed under the MIT License.
