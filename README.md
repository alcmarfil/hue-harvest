# Hue Harvest

**Hue Harvest** is a tactical, real-time 4-player game built with **Java Swing**. Players compete to claim territory in a 30x30 grid-based arena by leaving trails of their unique colors and using "Ink-Burst" abilities.

## Features 
- **Real-time Movement**: WASD or Arrow Keys for fluid character control.
- **Dynamic Grid**: Instantly capture tiles as you move.
- **Ink-Burst Ability**: Capture a 3x3 area with a 5-second cooldown (Spacebar).
- **Tactical UI**: Live leaderboard, match timer, and player chat.
- **Asset Ready**: Built-in support for custom 4-directional sprites and tile textures.

## 🛠️ Tech Stack
- **Language**: Java 17+
- **GUI Framework**: Java Swing (AWT)
- **Build System**: Apache Maven

---

## Getting Started
If you have Java and Maven installed, simply run:
```bash
mvn compile exec:java
```

---

## References & Documentation
Here are the official resources used to build Hue Harvest:

### Java & Swing
- [Java SE Documentation](https://docs.oracle.com/en/java/javase/) 
- [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/) 
- [Graphics2D Class Ref](https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html) - Documentation for the advanced rendering engine used in the game.

### Build Tools & Management
- [Apache Maven Documentation](https://maven.apache.org/guides/index.html) - Guides for the project management and build tool.
- [Maven Exec Plugin](https://www.mojohaus.org/exec-maven-plugin/) - Documentation for the plugin used to run the game from the terminal.
