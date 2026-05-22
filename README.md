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

## 🛠️ Prerequisites & Installation (By OS)

Before compiling or running the game, you need to install **Java Development Kit (JDK) 17+** and **Apache Maven** on your machine.

### 🍎 macOS
If you have **Homebrew** installed, run the following in your Terminal:
```bash
brew install openjdk@17 maven
```
Otherwise, download the installer directly:
- **JDK 17**: Download the macOS installer from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17&os=mac).
- **Maven**: Install via Homebrew or follow manual instructions [here](https://maven.apache.org/install.html).

### 🪟 Windows
You can install using a package manager like **Winget** or install manually:
#### Option 1: Via Winget (PowerShell / Command Prompt)
```powershell
winget install Eclipse.Temurin.JDK.17
winget install Apache.Maven
```
*Note: Restart your terminal after installation.*

#### Option 2: Manual Download
- **JDK 17**: Download and run the Windows `.msi` installer from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17&os=windows).
- **Maven**: Download the zip from [Apache Maven](https://maven.apache.org/download.cgi), extract it, and add its `bin` directory to your system's `PATH` environment variable.

### 🐧 Linux
Use your package manager to install JDK 17 and Maven:

#### Ubuntu / Debian / Mint:
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven -y
```

#### Arch Linux / Manjaro:
```bash
sudo pacman -S jdk17-openjdk maven
```

#### Fedora / RHEL:
```bash
sudo dnf install java-17-openjdk-devel maven -y
```

---

## 📦 Compilation

Once Java and Maven are installed, clone this repository, navigate to the directory, and build the project:

```bash
# Clean previous builds and compile dependencies
mvn clean package
```

---

## 🚀 Running the Game

Hue Harvest automatically manages servers and clients. In most cases, players only need to run the standard game client:

```bash
mvn compile exec:java
```

### Option A: Hosting a Game (Host)
1. Run `mvn compile exec:java` to start the game client.
2. Select **Enter Lobby** from the main menu.
3. Select **Host Game** (the client will automatically start a game server in the background on port `12345`).
4. Share your room code or IP address with other players.
5. Press **[Enter]** when you are ready to start the game.

### Option B: Joining a Game (Players)
1. Run `mvn compile exec:java` to start the game client.
2. Select **Enter Lobby** from the main menu.
3. Select **Join Game**.
4. Enter the Room Code (or IP address) provided by the host.

### Option C: Running a Dedicated Server (Optional)
If you want to run a standalone server without a client UI running on that machine, run:
```bash
mvn exec:java -Dexec.mainClass="com.hueharvest.server.GameServer"
```

---

## 🌐 Multiplayer Connectivity Modes

You can play Hue Harvest with friends over a Local Area Network (LAN) or over the Internet (WAN).

### Mode A: Local Area Network (LAN)
1. Ensure all players are connected to the same Wi-Fi/network.
2. The host clicks **Host Game** on their client.
3. The host finds their local IP address:
   - **macOS/Linux**: Run `ipconfig getifaddr en0` or `hostname -I`
   - **Windows**: Run `ipconfig` in Command Prompt
4. Share the IP address or Room Code with other players to connect.

### Mode B: Over the Internet (Remote Tunneling via Pinggy)
If players are not on the same network, you can use a tunnel service like [Pinggy](https://pinggy.io) to forward the port without configuring router firewalls.

1. The host starts the game and clicks **Host Game**.
2. The host runs the following SSH command in a separate terminal to set up a public TCP tunnel on port `12345`:
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
