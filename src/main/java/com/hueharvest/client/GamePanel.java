package com.hueharvest.client;

import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private Runnable restartAction;
    private Runnable winListener;

    public void setRestartAction(Runnable action) {
        this.restartAction = action;
    }

    public void setWinListener(Runnable listener) {
        this.winListener = listener;
    }

    private static final int TILE_SIZE = 40; 
    private static final double PLAYER_VISUAL_SCALE = 1.4; // Player is 40% larger than a tile
    private static final int ARC_SIZE = 12; // Visual Polish: Rounded corner radius
    private final GameState gameState;
    
    // Player State
    private int playerX = 0; 
    private int playerY = 0; 
    private int dirX = 0; 
    private int dirY = 1; 
    private final int playerId = 1; 

    // Visual Polish: Array to track the "Pop" scale of each tile (1.0 = normal)
    private float[][] popScale;

    // Burst Ability Logic
    private long lastBurstTime = 0;
    private static final long BURST_COOLDOWN = 5000;

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        
        // Initialize the pop effects grid to default scale
        this.popScale = new float[GameState.GRID_SIZE][GameState.GRID_SIZE];
        for(int r = 0; r < GameState.GRID_SIZE; r++) {
            for(int c = 0; c < GameState.GRID_SIZE; c++) popScale[r][c] = 1.0f;
        }

        setPreferredSize(new Dimension(GameState.GRID_SIZE * TILE_SIZE, GameState.GRID_SIZE * TILE_SIZE));
        setBackground(Color.WHITE);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // check for Exit/Restart keys if the game is over
                if (isGameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        System.exit(0); 
                    }
                    if (e.getKeyCode() == KeyEvent.VK_R) {
                        isGameOver = false; 
                        reset();
                        if (restartAction != null) restartAction.run();
                    }
                    return; 
                }

                // normal Gameplay Controls
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    fireInkBurst();
                } else {
                    movePlayer(e.getKeyCode());
                }
            }
        });
        // initialize starting position
        claimTile(playerX, playerY);
    }

    /**
     * Logic for claiming a tile and triggering the Visual Polish effect.
     */
    private void claimTile(int x, int y) {
        if (x < 0 || x >= GameState.GRID_SIZE || y < 0 || y >= GameState.GRID_SIZE) return;
        
        int currentOwner = gameState.getTile(x, y);
        if (currentOwner != playerId) {
            gameState.setTile(x, y, playerId);
            popScale[y][x] = 1.3f; 
            
            // immediate check if there is a win
            checkInstantWin(); 
        }
    }

    private void checkInstantWin() {
        if (gameState.getTileCount(playerId) >= GameState.GOAL_TILES) {
            if (winListener != null) {
                winListener.run();
            }
        }
    }

    private void fireInkBurst() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBurstTime < BURST_COOLDOWN) return;

        int targetX = playerX + (dirX * 3);
        int targetY = playerY + (dirY * 3);

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                claimTile(targetX + dx, targetY + dy);
            }
        }
        lastBurstTime = currentTime;
        repaint();
    }

    private void movePlayer(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> { dirX = 0; dirY = -1; }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> { dirX = 0; dirY = 1; }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> { dirX = -1; dirY = 0; }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> { dirX = 1; dirY = 0; }
        }

        playerX = Math.max(0, Math.min(GameState.GRID_SIZE - 1, playerX + dirX));
        playerY = Math.max(0, Math.min(GameState.GRID_SIZE - 1, playerY + dirY));

        claimTile(playerX, playerY);
        repaint();
    }

    private boolean isGameOver = false;
    private int finalRank = 0;
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
        drawPlayer(g2d);

        // Process frame-based animations
        updateAnimations();
        // CUSTOM RESULT MODAL
        if (isGameOver) {
            drawResultModal(g2d);
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
        int tileType = gameState.getTile(x, y);
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

    private void drawPlayer(Graphics2D g2d) {
        String directionSuffix = getDirectionSuffix();
        BufferedImage playerImg = AssetManager.getImage("player" + playerId + "_" + directionSuffix + ".png");
        int drawSize = (int) (TILE_SIZE * PLAYER_VISUAL_SCALE);
        int offset = (drawSize - TILE_SIZE) / 2;

        if (playerImg != null) {
            g2d.drawImage(playerImg, playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize, null);
        } else {
            // Character Renderer fallback
            g2d.setColor(Color.BLACK);
            g2d.fillOval(playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize);
        }
    }

    /**
     * Task: Visual Polish - Manages the transition of the pop effect back to normal size.
     */
    private void updateAnimations() {
        boolean animating = false;
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                if (popScale[y][x] > 1.0f) {
                    popScale[y][x] -= 0.02f; // Animation speed for the shrink-back
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

    private String getDirectionSuffix() {
        if (dirX == 1) return "right";
        if (dirX == -1) return "left";
        if (dirY == 1) return "down";
        if (dirY == -1) return "up";
        return "down";
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
        long elapsed = System.currentTimeMillis() - lastBurstTime;
        return Math.max(0, (BURST_COOLDOWN - elapsed) / 1000);
    }

    public void reset() {
        this.isGameOver = false; 
        this.playerX = 0;
        this.playerY = 0;
        this.dirX = 0;
        this.dirY = 1;
        this.lastBurstTime = 0;

        // clear actual game board
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                gameState.setTile(x, y, 0); 
            }
        }
        // reset the pop scales
        for (int r = 0; r < GameState.GRID_SIZE; r++) {
            for (int c = 0; c < GameState.GRID_SIZE; c++) popScale[r][c] = 1.0f;
        }
        gameState.setTile(0, 0, playerId);
        
        repaint();
    }
}