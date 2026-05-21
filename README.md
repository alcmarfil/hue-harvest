# 🌾 Hue Harvest

**Hue Harvest** is a tactical, real-time 4-player multiplayer game built with **Java Swing** and **Java Sockets**. Players compete to claim territory in a 30x30 grid-based arena by leaving trails of their unique colors and using high-impact **Ink-Burst** abilities.

---

## 🎮 Game Controls & Features

### In-Game Controls
- **Movement**: Use **WASD** or **Arrow Keys** to steer your harvester.
- **Ink-Burst**: Press **Spacebar** to instantly capture a $3\times3$ grid around your target trajectory (5-second cooldown).
- **Chat**: Use the sidebar text input to chat with other players in real-time.

### Lobby & Match Navigation
- **Start Match (Host Only)**: Press **[Enter]** in the waiting room/lobby.
- **Re-sow (Rematch)**: Press **[R]** at the results screen. When all connected players vote to re-sow, the game instantly restarts.
- **Exit to Homepage**: Press **[ESC]** at any point (waiting room, active match, or results screen) to bring up a confirmation dialog to return to the homepage. Exiting closes the lobby.
- **Quit Game**: Press **[ESC]** on the Title Screen to exit the application.

---

## 🛠️ Tech Stack & Prerequisites

Before running the game, ensure you have the following installed:
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Apache Maven**: Build and dependency management tool.

---

## 📦 Compilation & Installation

Clone this repository and compile the source code using Maven:

```bash
# Clean previous builds and package into a JAR
mvn clean package
```

---

## 🚀 Running the Game

Hue Harvest uses a Client-Server architecture. To play multiplayer, you must start the server first, followed by the game clients.

### 1. Start the Server
Run the game server on the hosting machine:
```bash
mvn exec:java -Dexec.mainClass="com.hueharvest.server.GameServer"
```
*Note: The server listens on TCP port `12345` by default.*

### 2. Start the Client
Run the game client (each player opens their own client):
```bash
mvn exec:java -Dexec.mainClass="com.hueharvest.Main"
```

---

## 🌐 Multiplayer Connectivity Modes

You can play Hue Harvest with friends over a Local Area Network (LAN) or over the Internet (WAN).

### Mode A: Local Area Network (LAN)
1. Ensure all players are connected to the same Wi-Fi network.
2. The host runs the **Server**.
3. The host finds their local IP address:
   - **macOS/Linux**: Run `ipconfig getifaddr en0` or `hostname -I`
   - **Windows**: Run `ipconfig` in Command Prompt
4. Share the IP address with the other players.
5. In the Client menu, other players select **Join Game** and enter the room code displayed by the host.

### Mode B: Over the Internet (Remote Tunneling via Pinggy)
If players are not on the same network, you can use a tunnel service like [Pinggy](https://pinggy.io) to forward the port without configuring router firewalls.

1. The host starts the **Game Server** locally.
2. The host runs the following SSH command to set up a public TCP tunnel on port `12345`:
   ```bash
   ssh -p 443 -o StrictHostKeyChecking=no -R0:localhost:12345 tcp@a.pinggy.io
   ```
3. Pinggy will output a public TCP URL (e.g. `tcp://tcp.pinggy.link:45678`).
4. Other players copy the port (`45678`) and enter the corresponding Room Code into their clients to connect instantly over the internet!

---

## 📂 Project Architecture

```
src/main/java/com/hueharvest/
├── client/
│   ├── AssetManager.java     # Assets caching (sprites, icons, maps)
│   ├── GamePanel.java        # Core game canvas & rendering
│   └── SoundManager.java     # Sound effects
├── server/
│   └── GameServer.java       # Sockets server & client handler threads
├── shared/
│   ├── GameState.java        # Serialized game state (positions, tiles grid, status)
│   ├── NetworkPacket.java    # Sockets transport packet wrapper
│   └── NetworkUtils.java     # Helper utilities for IP conversion
└── Main.java                 # Entry point & Swing UI views (Title, Menu, Join Screen)
```

---

## 📢 Public Repository Access
To share this game, make sure to set the repository settings to **Public** on GitHub:
1. Navigate to your repository page.
2. Click on **Settings** (gear icon) in the top menu bar.
3. Scroll down to the **Danger Zone** section.
4. Click **Change visibility** -> **Make public** and follow the prompts.
