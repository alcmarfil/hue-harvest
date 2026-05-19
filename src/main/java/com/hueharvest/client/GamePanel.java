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
    
    // Local Prediction for the current player to eliminate lag
    private int localX = -1, localY = -1;

    public int getMyPlayerId() {
        return myPlayerId;
    }

    public boolean isBurstReady() {
        if (remoteGameState == null || myPlayerId == -1) return true;
        return remoteGameState.isBurstReady(myPlayerId);
    }

    public GameState getRemoteGameState() {
        return remoteGameState;
    }

    public void setMessageListener(java.util.function.Consumer<String> listener) {
        this.messageListener = listener;
    }

    private static final int TILE_SIZE = 40; 
    private static final double PLAYER_VISUAL_SCALE = 1.4; // Player is 40% larger than a tile
    private static final int ARC_SIZE = 12; // Visual Polish: Rounded corner radius
    
    // Visual Polish: Array to track the "Pop" scale of each tile (1.0 = normal)
    private float[][] popScale;

    // Burst Ability Logic
    private static final long BURST_COOLDOWN = 5000;

    public GamePanel(GameState gameState) {
        this.remoteGameState = gameState;
        
        // Initialize the pop effects grid to default scale
        this.popScale = new float[GameState.GRID_SIZE][GameState.GRID_SIZE];
        for(int r = 0; r < GameState.GRID_SIZE; r++) {
            for(int c = 0; c < GameState.GRID_SIZE; c++) popScale[r][c] = 1.0f;
        }

        setPreferredSize(new Dimension(GameState.GRID_SIZE * TILE_SIZE, GameState.GRID_SIZE * TILE_SIZE));
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
                    if (e.getKeyCode() == KeyEvent.VK_R && myPlayerId == 1) {
                        // Reset request to server (could add a RESET packet type, but for now just restart)
                        sendPacket(new NetworkPacket(NetworkPacket.Type.START, myPlayerId, null));
                        isGameOver = false;
                    }
                    return;
                }

                // Client Input Handling
                if (remoteGameState.getStatus() == GameState.Status.LOBBY) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER && myPlayerId == 1) {
                        sendPacket(new NetworkPacket(NetworkPacket.Type.START, myPlayerId, null));
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (isBurstReady()) {
                        // Local Prediction for Burst
                        triggerLocalBurst();
                        sendPacket(new NetworkPacket(NetworkPacket.Type.BURST, myPlayerId, null));
                    }
                } else {
                    int dx = 0, dy = 0;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W, KeyEvent.VK_UP -> dy = -1;
                        case KeyEvent.VK_S, KeyEvent.VK_DOWN -> dy = 1;
                        case KeyEvent.VK_A, KeyEvent.VK_LEFT -> dx = -1;
                        case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> dx = 1;
                    }
                    if (dx != 0 || dy != 0) {
                        // Local Prediction: Update position immediately for visual feedback
                        if (localX != -1 && localY != -1) {
                            int nextX = Math.max(0, Math.min(GameState.GRID_SIZE - 1, localX + dx));
                            int nextY = Math.max(0, Math.min(GameState.GRID_SIZE - 1, localY + dy));
                            localX = nextX;
                            localY = nextY;
                            repaint();
                        }
                        sendPacket(new NetworkPacket(NetworkPacket.Type.MOVE, myPlayerId, new int[]{dx, dy}));
                    }
                }
            }
        });

        // Dedicate a timer for all visual updates to keep the UI responsive
        new Timer(16, e -> {
            if (remoteGameState != null) {
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

    private void triggerLocalBurst() {
        if (localX == -1 || localY == -1) return;
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
                        
                        // Sync local prediction with server only if discrepancy is large to avoid jitter
                        if (myPlayerId != -1) {
                            int sX = remoteGameState.getPlayerX(myPlayerId);
                            int sY = remoteGameState.getPlayerY(myPlayerId);
                            // Relaxed sync threshold to prevent "elastic" snapping
                            if (localX == -1 || localY == -1 || Math.abs(localX - sX) > 3 || Math.abs(localY - sY) > 3) {
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
                        if (chatMessages.size() > 5) chatMessages.remove(0);
                        if (messageListener != null) messageListener.accept(msg);
                        repaint();
                    }
                }
            } catch (Exception e) {
                if (myPlayerId == -1) {
                    String msg = e.getMessage();
                    if (msg == null || msg.equals("null")) msg = "Could not reach the server.";
                    JOptionPane.showMessageDialog(this, "Connection failed: " + msg);
                } else {
                    String msg = e.getMessage();
                    if (e instanceof java.io.EOFException || msg == null || msg.equals("null") || msg.contains("Socket closed") || msg.contains("Connection reset")) {
                        msg = "The connection to the server was lost.";
                    }
                    JOptionPane.showMessageDialog(this, msg, "Connection Lost", JOptionPane.WARNING_MESSAGE);
                }
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
        
        // Icon Placeholder 
        g2d.setColor(new Color(210, 180, 140)); 
        g2d.fillOval(mx + 70, my + 165, 40, 40);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.setColor(new Color(101, 67, 33)); 
        g2d.drawString(finalScore + " CROPS", mx + 130, my + 195);

        // 7. Footer Instructions
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.setColor(new Color(120, 140, 120));
        drawCenteredString(g2d, "Press [R] to Re-sow  •  [ESC] to Quit", mx, my + 270, mW);

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
        
        // Don't draw players that haven't moved/connected yet (initial pos 0,0 but pId != 1)
        if (pX == 0 && pY == 0 && pId != 1 && remoteGameState.getTile(0, 0) != pId) return;

        String directionSuffix = getDirectionSuffix(dX, dY);
        BufferedImage playerImg = AssetManager.getImage("player" + pId + "_" + directionSuffix + ".png");
        int drawSize = (int) (TILE_SIZE * PLAYER_VISUAL_SCALE);
        int offset = (drawSize - TILE_SIZE) / 2;

        if (playerImg != null) {
            g2d.drawImage(playerImg, pX * TILE_SIZE - offset, pY * TILE_SIZE - offset, drawSize, drawSize, null);
        } else {
            // Character Renderer fallback
            g2d.setColor(getColorForPlayer(pId));
            g2d.fillOval(pX * TILE_SIZE - offset, pY * TILE_SIZE - offset, drawSize, drawSize);
            
            // Small indicator of direction
            g2d.setColor(Color.BLACK);
            int eyeSize = 6;
            int ex = pX * TILE_SIZE + TILE_SIZE / 2 + dX * 10 - eyeSize / 2;
            int ey = pY * TILE_SIZE + TILE_SIZE / 2 + dY * 10 - eyeSize / 2;
            g2d.fillOval(ex, ey, eyeSize, eyeSize);
        }
    }

    private String getDirectionSuffix(int dX, int dY) {
        if (dX == 1) return "right";
        if (dX == -1) return "left";
        if (dY == 1) return "down";
        if (dY == -1) return "up";
        return "down";
    }

    /**
     * Task: Visual Polish - Manages the transition of the pop effect back to normal size.
     */
    private void updateAnimations() {
        boolean animating = false;
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                if (popScale[y][x] > 1.0f) {
                    popScale[y][x] -= 0.08f; // Faster decay for snappier feel
                    if (popScale[y][x] < 1.0f) popScale[y][x] = 1.0f;
                    animating = true;
                } else {
                    popScale[y][x] = 1.0f;
                }
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