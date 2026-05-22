package com.hueharvest.client;

import com.hueharvest.shared.GameState;
import com.hueharvest.shared.NetworkPacket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private int myPlayerId = -1;
    private ObjectOutputStream out;
    private GameState remoteGameState; // State received from server
    private String serverIp = "localhost";
    private final List<String> chatMessages = new ArrayList<>();
    private java.util.function.Consumer<String> messageListener;
    private final java.util.Map<Integer, Integer> playerFrameIndex = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> lastFrameTime = new java.util.HashMap<>();
    private final int[] walkSequence = {0, 1, 2, 1};

    // Local Prediction for the current player to eliminate lag
    private int localX = -1, localY = -1;
    private final java.util.Set<Integer> pressedKeys = new java.util.HashSet<>();
    private long lastMoveTime = 0;
    private static final long MOVE_COOLDOWN = 110; // ms between continuous steps

    public int getMyPlayerId() {
        return myPlayerId;
    }

    public boolean isBurstReady() {
        if (remoteGameState == null || myPlayerId == -1)
            return true;
        return remoteGameState.isBurstReady(myPlayerId);
    }

    public GameState getRemoteGameState() {
        return remoteGameState;
    }

    public void setMessageListener(java.util.function.Consumer<String> listener) {
        this.messageListener = listener;
    }

    private Runnable onQuitCallback;

    public void setOnQuitCallback(Runnable callback) {
        this.onQuitCallback = callback;
    }

    private static final int TILE_SIZE = 40;
    private static final double PLAYER_VISUAL_SCALE = 1.4; // Player is 40% larger than a tile
    private static final int ARC_SIZE = 12; // Visual Polish: Rounded corner radius

    // Visual Polish: Array to track the "Pop" scale of each tile (1.0 = normal)
    private float[][] popScale;

    // Burst Ability Logic

    public GamePanel(GameState gameState) {
        this.remoteGameState = gameState;

        // Initialize the pop effects grid to default scale
        this.popScale = new float[GameState.GRID_SIZE][GameState.GRID_SIZE];
        for (int r = 0; r < GameState.GRID_SIZE; r++) {
            for (int c = 0; c < GameState.GRID_SIZE; c++)
                popScale[r][c] = 1.0f;
        }

        setPreferredSize(new Dimension(GameState.GRID_SIZE * TILE_SIZE, GameState.GRID_SIZE * TILE_SIZE));
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (isGameOver) {
                    if (key == KeyEvent.VK_ESCAPE) {
                        sendPacket(new NetworkPacket(NetworkPacket.Type.QUIT, myPlayerId, null));
                    }
                    if (key == KeyEvent.VK_R) {
                        sendPacket(new NetworkPacket(NetworkPacket.Type.REMATCH, myPlayerId, null));
                    }
                    return;
                }

                // Client Input Handling
                if (remoteGameState.getStatus() == GameState.Status.LOBBY) {
                    if (key == KeyEvent.VK_ENTER && myPlayerId == 1) {
                        sendPacket(new NetworkPacket(NetworkPacket.Type.START, myPlayerId, null));
                    }
                }

                if (key == KeyEvent.VK_SPACE) {
                    if (isBurstReady()) {
                        // Local Prediction for Burst
                        triggerLocalBurst();
                        sendPacket(new NetworkPacket(NetworkPacket.Type.BURST, myPlayerId, null));
                    }
                }

                if (isMovementKey(key)) {
                    synchronized (pressedKeys) {
                        pressedKeys.add(key);
                    }
                    processMovementTick();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (isMovementKey(key)) {
                    synchronized (pressedKeys) {
                        pressedKeys.remove(key);
                    }
                }
            }

            private boolean isMovementKey(int key) {
                return key == KeyEvent.VK_W || key == KeyEvent.VK_UP ||
                       key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN ||
                       key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT ||
                       key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT;
            }
        });

        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                synchronized (pressedKeys) {
                    pressedKeys.clear();
                }
            }
        });

        // Dedicate a timer for all visual updates to keep the UI responsive
        new Timer(16, e -> {
            if (remoteGameState != null) {
                processMovementTick();
                updateAnimations();
                repaint();
            }
        }).start();

        // Ensure we always have focus for movement
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    private void processMovementTick() {
        if (remoteGameState == null || remoteGameState.getStatus() != GameState.Status.PLAYING || isGameOver) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastMoveTime < MOVE_COOLDOWN) {
            return;
        }

        int dx = 0;
        int dy = 0;

        synchronized (pressedKeys) {
            if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) {
                dy = -1;
            } else if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) {
                dy = 1;
            }

            if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) {
                dx = -1;
            } else if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) {
                dx = 1;
            }
        }

        if (dx != 0 || dy != 0) {
            lastMoveTime = now;
            if (localX != -1 && localY != -1) {
                int nextX = Math.max(0, Math.min(GameState.GRID_SIZE - 1, localX + dx));
                int nextY = Math.max(0, Math.min(GameState.GRID_SIZE - 1, localY + dy));
                localX = nextX;
                localY = nextY;
                repaint();
            }
            sendPacket(new NetworkPacket(NetworkPacket.Type.MOVE, myPlayerId, new int[] { dx, dy }));
        }
    }

    private void triggerLocalBurst() {
        if (localX == -1 || localY == -1)
            return;
        int dX = remoteGameState.getDirX(myPlayerId);
        int dY = remoteGameState.getDirY(myPlayerId);
        int targetX = localX + (dX * 3);
        int targetY = localY + (dY * 3);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = targetX + dx;
                int ty = targetY + dy;
                if (tx >= 0 && tx < GameState.GRID_SIZE && ty >= 0 && ty < GameState.GRID_SIZE) {
                    popScale[ty][tx] = 1.4f;
                }
            }
        }
    }

    public void connect(String host) {
        this.serverIp = host;
        new Thread(() -> {
            String ip = host;
            int port = 12345;
            if (host.contains(":")) {
                String[] parts = host.split(":");
                ip = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                }
            }
            try (Socket socket = new Socket(ip, port)) {
                socket.setTcpNoDelay(true); // Disable Nagle's algorithm for instant packet delivery
                out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    NetworkPacket packet = (NetworkPacket) in.readObject();
                    if (packet.type == NetworkPacket.Type.WELCOME) {
                        myPlayerId = packet.playerId;
                        if (myPlayerId == -1) {
                            String reason = packet.data != null ? packet.data.toString() : "Server is full.";
                            throw new Exception(reason);
                        }
                    } else if (packet.type == NetworkPacket.Type.UPDATE) {
                        remoteGameState = (GameState) packet.data;

                        if (remoteGameState.getStatus() == GameState.Status.PLAYING) {
                            isGameOver = false; // Reset game over screen when server restarts
                        }

                        // Sync local prediction with server only if discrepancy is large to avoid
                        // jitter
                        if (myPlayerId != -1) {
                            int sX = remoteGameState.getPlayerX(myPlayerId);
                            int sY = remoteGameState.getPlayerY(myPlayerId);
                            // Relaxed sync threshold to prevent "elastic" snapping
                            if (localX == -1 || localY == -1 || Math.abs(localX - sX) > 3
                                    || Math.abs(localY - sY) > 3) {
                                localX = sX;
                                localY = sY;
                            }
                        }

                        if (remoteGameState.getStatus() == GameState.Status.FINISHED && !isGameOver) {
                            int winnerId = remoteGameState.getWinnerId();
                            String msg = winnerId == -1 ? "Time's Up!" : "Player " + winnerId + " Won!";
                            setGameOver(winnerId == myPlayerId, msg, remoteGameState.getTileCount(myPlayerId));
                        }
                    } else if (packet.type == NetworkPacket.Type.CHAT) {
                        String sender = (packet.playerId == myPlayerId) ? "You" : "Player " + packet.playerId;
                        String msg = sender + ": " + packet.data;
                        chatMessages.add(msg);
                        if (chatMessages.size() > 5)
                            chatMessages.remove(0);
                        if (messageListener != null)
                            messageListener.accept(msg);
                        repaint();
                    } else if (packet.type == NetworkPacket.Type.QUIT) {
                        isGameOver = false;
                        try {
                            socket.close();
                        } catch (Exception ex) {
                        }
                        if (onQuitCallback != null) {
                            SwingUtilities.invokeLater(onQuitCallback);
                        }
                        if (packet.playerId != myPlayerId) {
                            JOptionPane.showMessageDialog(this,
                                    "Player " + packet.playerId + " returned to the homepage. Lobby closed.",
                                    "Lobby Closed", JOptionPane.INFORMATION_MESSAGE);
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                if (myPlayerId == -1) {
                    String msg = e.getMessage();
                    if (msg == null || msg.equals("null"))
                        msg = "Could not reach the server.";
                    JOptionPane.showMessageDialog(this, "Connection failed: " + msg);
                } else {
                    String msg = e.getMessage();
                    if (e instanceof java.io.EOFException || msg == null || msg.equals("null")
                            || msg.contains("Socket closed") || msg.contains("Connection reset")) {
                        msg = "The connection to the server was lost.";
                    }
                    JOptionPane.showMessageDialog(this, msg, "Connection Lost", JOptionPane.WARNING_MESSAGE);
                }
            } finally {
                out = null;
                myPlayerId = -1;
                chatMessages.clear();
                localX = -1;
                localY = -1;
                remoteGameState = new GameState();
                repaint();
            }
        }).start();
    }

    public synchronized void sendPacket(NetworkPacket packet) {
        if (out != null) {
            try {
                out.writeUnshared(packet);
                out.flush();
                out.reset();
            } catch (IOException e) {
                // Silently ignore write failures on closed sockets
            }
        }
    }

    private boolean isGameOver = false;
    private int finalScore = 0;
    private String gameOverSubtitle = "";
    private String gameOverTitle = "";

    public void setGameOver(boolean won, String subtitle, int finalScore) {
        this.isGameOver = true;
        this.gameOverTitle = won ? "VICTORY!" : "MATCH EXPIRED";
        this.gameOverSubtitle = subtitle;
        this.finalScore = finalScore;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Antialiasing for smooth rounded corners and high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Iterate through grid and draw tiles
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                drawTile(g2d, x, y);
            }
        }

        // Draw character sprites
        for (int i = 1; i <= 4; i++) {
            drawPlayer(g2d, i);
        }

        // Process frame-based animations
        updateAnimations();

        if (remoteGameState.getStatus() == GameState.Status.LOBBY) {
            drawLobbyOverlay(g2d);
        }

        // CUSTOM RESULT MODAL
        if (isGameOver) {
            drawResultModal(g2d);
        }
    }

    private void drawLobbyOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawCenteredString(g2d, "WAITING ROOM", 0, getHeight() / 2 - 80, getWidth());

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        String displayIp = serverIp;
        if (displayIp.equals("localhost") || displayIp.equals("127.0.0.1")) {
            displayIp = com.hueharvest.shared.NetworkUtils.getLocalNetworkIp();
        }
        String roomCode = com.hueharvest.shared.NetworkUtils.ipToRoomCode(displayIp);

        drawCenteredString(g2d, "Room Code: " + roomCode, 0, getHeight() / 2 - 30, getWidth());
        drawCenteredString(g2d, "IP Address: " + displayIp, 0, getHeight() / 2 + 5, getWidth());

        int playerCount = remoteGameState.getNumPlayers();
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCenteredString(g2d, "Players Connected: " + playerCount + " / 4", 0, getHeight() / 2 + 45, getWidth());

        if (myPlayerId == 1) {
            g2d.setColor(new Color(152, 251, 152));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
            drawCenteredString(g2d, "Press [ENTER] to Start Game", 0, getHeight() / 2 + 100, getWidth());
        } else {
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            drawCenteredString(g2d, "Waiting for host to start...", 0, getHeight() / 2 + 100, getWidth());
        }
    }

    private void drawResultModal(Graphics2D g2d) {
        // Soft Overlay
        g2d.setColor(new Color(30, 50, 40, 160));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Modal Dimensions
        int mW = 400, mH = 320;
        int mx = (getWidth() - mW) / 2;
        int my = (getHeight() - mH) / 2;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Border
        g2d.setColor(new Color(143, 188, 143)); // SeaGreen/Sage
        g2d.fillRoundRect(mx - 6, my - 6, mW + 12, mH + 12, 40, 40);

        // Main Body
        g2d.setColor(new Color(252, 249, 237));
        g2d.fillRoundRect(mx, my, mW, mH, 35, 35);

        // Header Ribbon
        boolean isVictory = gameOverTitle.equals("VICTORY!");
        Color accentColor = isVictory ? new Color(108, 153, 108) : new Color(204, 115, 115);

        g2d.setColor(accentColor);
        g2d.fillRoundRect(mx + 60, my + 30, mW - 120, 50, 25, 25);

        // Title
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        drawCenteredString(g2d, gameOverTitle, mx, my + 65, mW);

        // Subtitle
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2d.setColor(new Color(60, 80, 60));
        drawCenteredString(g2d, gameOverSubtitle, mx, my + 120, mW);

        // Score Area
        g2d.setColor(new Color(235, 230, 210));
        g2d.fillRoundRect(mx + 50, my + 150, mW - 100, 70, 20, 20);

        // Dynamic Rank Calculation
        int rank = 1;
        int pId = myPlayerId != -1 ? myPlayerId : 1;
        if (remoteGameState != null) {
            int myScore = remoteGameState.getTileCount(pId);
            for (int i = 1; i <= 4; i++) {
                if (i != pId) {
                    int otherScore = remoteGameState.getTileCount(i);
                    if (otherScore > myScore) {
                        rank++;
                    } else if (otherScore == myScore && i < pId) {
                        rank++;
                    }
                }
            }
        }

        String rankStr = switch (rank) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            case 4 -> "4th";
            default -> rank + "th";
        };

        Color medalBg;
        Color medalFg;
        switch (rank) {
            case 1 -> {
                medalBg = new Color(255, 215, 0); // Gold
                medalFg = new Color(139, 101, 8);
            }
            case 2 -> {
                medalBg = new Color(192, 192, 192); // Silver
                medalFg = new Color(90, 90, 90);
            }
            case 3 -> {
                medalBg = new Color(205, 127, 50); // Bronze
                medalFg = new Color(120, 60, 20);
            }
            default -> {
                medalBg = new Color(176, 190, 197); // Slate/Steel
                medalFg = new Color(80, 90, 100);
            }
        }

        // Draw Medal Badge
        g2d.setColor(medalBg);
        g2d.fillOval(mx + 70, my + 165, 40, 40);

        g2d.setColor(medalFg);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics medalMetrics = g2d.getFontMetrics();
        int rx = mx + 70 + (40 - medalMetrics.stringWidth(rankStr)) / 2;
        int ry = my + 165 + (40 - medalMetrics.getHeight()) / 2 + medalMetrics.getAscent();
        g2d.drawString(rankStr, rx, ry);

        // Rank Title Text
        g2d.setFont(new Font("Poppins", Font.BOLD, 15));
        g2d.setColor(medalFg);
        g2d.drawString(rankStr + " Place", mx + 130, my + 182);

        // Crop Count Text
        g2d.setFont(new Font("Poppins", Font.BOLD, 18));
        g2d.setColor(new Color(101, 67, 33));
        g2d.drawString(finalScore + " CROPS", mx + 130, my + 203);

        // 7. Footer Instructions
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.setColor(new Color(120, 140, 120));
        drawCenteredString(g2d, "Press [R] to Re-sow  •  [ESC] to Exit", mx, my + 270, mW);

        // Decorative "Sprout" (Bottom corner detail)
        g2d.setColor(new Color(143, 188, 143));
        g2d.fillOval(mx + mW - 40, my + mH - 40, 20, 20);
    }

    private void drawCenteredString(Graphics2D g, String text, int x, int y, int width) {
        FontMetrics metrics = g.getFontMetrics();
        int tx = x + (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, tx, y);
    }

    /**
     * Draws individual tiles with rounded corners and scaling effects.
     */
    private void drawTile(Graphics2D g2d, int x, int y) {
        int tileType = remoteGameState.getTile(x, y);
        float scale = popScale[y][x];

        // Calculate size/position
        int baseSize = (int) (TILE_SIZE * scale);
        int baseOffset = (baseSize - TILE_SIZE) / 2;
        int drawX = x * TILE_SIZE - baseOffset;
        int drawY = y * TILE_SIZE - baseOffset;

        // Create the rounded shape for clipping
        Shape roundRect = new RoundRectangle2D.Float(drawX, drawY, baseSize, baseSize, ARC_SIZE, ARC_SIZE);
        Shape oldClip = g2d.getClip();
        g2d.setClip(roundRect);

        // Draw the Grass (tile0) first as the base layer
        BufferedImage baseImg = AssetManager.getImage("tile0.png");
        if (baseImg != null) {
            g2d.drawImage(baseImg, drawX, drawY, baseSize, baseSize, null);
        } else {
            g2d.setColor(Color.WHITE); // Backup if grass is missing
            g2d.fill(roundRect);
        }

        // Draw the Player's Tile ON TOP if it's not neutral
        if (tileType != 0) {
            // Task: Renderer - Relative scaling for the crop overlay
            double cropScaleFactor = 0.8; // 80% of tile size
            int cropSize = (int) (baseSize * cropScaleFactor);

            // Centering the crop on top of the base tile
            int cropOffset = (baseSize - cropSize) / 2;
            int cropX = drawX + cropOffset;
            int cropY = drawY + cropOffset;

            BufferedImage cropImg = AssetManager.getImage("tile" + tileType + ".png");

            if (cropImg != null) {
                // Draw the actual crop asset (Tomato, Corn, etc.)
                g2d.drawImage(cropImg, cropX, cropY, cropSize, cropSize, null);
            } else {
                // Fallback: Use the player's theme color if the specific crop image is missing
                g2d.setColor(getColorForPlayer(tileType));
                g2d.fillOval(cropX, cropY, cropSize, cropSize);
            }
        }

        g2d.setClip(oldClip);
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.draw(roundRect);
    }

    private void drawPlayer(Graphics2D g2d, int pId) {
    int pX = (pId == myPlayerId && localX != -1) ? localX : remoteGameState.getPlayerX(pId);
    int pY = (pId == myPlayerId && localY != -1) ? localY : remoteGameState.getPlayerY(pId);
    int dX = remoteGameState.getDirX(pId);
    int dY = remoteGameState.getDirY(pId);

    // Initial check (kung di pa nag-move)
    if (pX == 0 && pY == 0 && pId != 1 && remoteGameState.getTile(0, 0) != pId) return;

    String dir = getDirName(dX, dY);
    
    int seqIndex = playerFrameIndex.getOrDefault(pId, 1);
    int actualFrame = walkSequence[seqIndex]; 
    
    String fileName = "player" + pId + "_" + dir + "_" + actualFrame + ".png";
    BufferedImage playerImg = AssetManager.getImage(fileName);
    
    int drawSize = (int) (TILE_SIZE * PLAYER_VISUAL_SCALE);
    int offset = (drawSize - TILE_SIZE) / 2;

    if (playerImg != null) {
        g2d.drawImage(playerImg, pX * TILE_SIZE - offset, pY * TILE_SIZE - offset, drawSize, drawSize, null);
    } else {
        g2d.setColor(getColorForPlayer(pId));
        g2d.fillOval(pX * TILE_SIZE - offset, pY * TILE_SIZE - offset, drawSize, drawSize);
    }
}
    private String getDirName(int dX, int dY) {
        if (dX == 1) return "right";
        if (dX == -1) return "left";
        if (dY == 1) return "down";
        if (dY == -1) return "up";
        return "down"; // Default
    }

    /**
     * Task: Visual Polish - Manages the transition of the pop effect back to normal
     * size.
     */
    private void updateAnimations() {
        boolean animating = false;
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                if (popScale[y][x] > 1.0f) {
                    popScale[y][x] -= 0.08f; // Faster decay for snappier feel
                    if (popScale[y][x] < 1.0f)
                        popScale[y][x] = 1.0f;
                    animating = true;
                } else {
                    popScale[y][x] = 1.0f;
                }
            }
        }

        long now = System.currentTimeMillis();
        int[] walkSequence = {0, 1, 2, 1}; // Sequence: Left, Static, Right, Static

        for (int pId = 1; pId <= 4; pId++) {
            if (remoteGameState.getDirX(pId) != 0 || remoteGameState.getDirY(pId) != 0) {
                if (now - lastFrameTime.getOrDefault(pId, 0L) > 150) {
                    int seqIndex = playerFrameIndex.getOrDefault(pId, 0);
                    
                    int nextSeqIndex = (seqIndex + 1) % 4;
                    playerFrameIndex.put(pId, nextSeqIndex);
                    
                    lastFrameTime.put(pId, now);
                    animating = true; 
                }
            } else {
                playerFrameIndex.put(pId, 1);
            }
        }

        // Refresh the panel if animations are still active
        if (animating) {
            Timer t = new Timer(16, e -> repaint());
            t.setRepeats(false);
            t.start();
        }
    }

    private Color getColorForPlayer(int id) {
        return switch (id) {
            case 1 -> new Color(255, 182, 193); // Player 1 Color
            case 2 -> new Color(152, 251, 152);
            case 3 -> new Color(230, 230, 250);
            case 4 -> new Color(135, 206, 235);
            default -> Color.WHITE; // Neutral Tile Color
        };
    }

    public long getCooldownRemaining() {
        // Simple cooldown check on client side for UI (server also checks)
        return 0; // Simplified for now
    }

    public void reset() {
        // Reset handled by server
    }
}