package com.hueharvest.client;

import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final int TILE_SIZE = 40; 
    private static final double PLAYER_VISUAL_SCALE = 1.4;
    private static final int ARC_SIZE = 12; // Visual Polish: Rounded corner radius
    private final GameState gameState;
    
    private int playerX = 0; 
    private int playerY = 0; 
    private int dirX = 0; 
    private int dirY = 1; 
    private final int playerId = 1; 

    // Visual Polish: Array to track the "Pop" scale of each tile (1.0 = normal)
    private float[][] popScale;

    private long lastBurstTime = 0;
    private static final long BURST_COOLDOWN = 5000;

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        // Initialize the pop effects grid
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
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    fireInkBurst();
                } else {
                    movePlayer(e.getKeyCode());
                }
            }
        });

        claimTile(playerX, playerY);
    }

    // Task: Visual Polish - Trigger pop when tile state changes
    private void claimTile(int x, int y) {
        if (x < 0 || x >= GameState.GRID_SIZE || y < 0 || y >= GameState.GRID_SIZE) return;
        
        int currentOwner = gameState.getTile(x, y);
        if (currentOwner != playerId) {
            gameState.setTile(x, y, playerId);
            popScale[y][x] = 1.3f; // Start the "pop" at 130% size
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Task: Renderer - Antialiasing for smooth rounded corners
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw grid and tiles
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                drawTile(g2d, x, y);
            }
        }

        // Draw player
        drawPlayer(g2d);

        // Update pop animations for next frame
        updateAnimations();
    }

    private void drawTile(Graphics2D g2d, int x, int y) {
        int tileType = gameState.getTile(x, y);
        float scale = popScale[y][x];
        
        // Calculate dynamic size/position for the pop effect
        int size = (int) (TILE_SIZE * scale);
        int offset = (size - TILE_SIZE) / 2;
        int drawX = x * TILE_SIZE - offset;
        int drawY = y * TILE_SIZE - offset;

        BufferedImage tileImg = AssetManager.getImage("tile" + tileType + ".png");
        
        // Task: Renderer - Create rounded corner shape
        Shape roundRect = new RoundRectangle2D.Float(drawX, drawY, size, size, ARC_SIZE, ARC_SIZE);

        if (tileImg != null) {
            Shape oldClip = g2d.getClip();
            g2d.setClip(roundRect); // Clips image into the rounded shape
            g2d.drawImage(tileImg, drawX, drawY, size, size, null);
            g2d.setClip(oldClip);
        } else {
            // Fallback for tile1-4 if Member A hasn't sent them yet
            g2d.setColor(getColorForPlayer(tileType));
            g2d.fill(roundRect);
            
            // Subtle Grid lines
            g2d.setColor(new Color(240, 240, 240, 150));
            g2d.draw(roundRect);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        String directionSuffix = getDirectionSuffix();
        BufferedImage playerImg = AssetManager.getImage("player" + playerId + "_" + directionSuffix + ".png");
        int drawSize = (int) (TILE_SIZE * PLAYER_VISUAL_SCALE);
        int offset = (drawSize - TILE_SIZE) / 2;

        if (playerImg != null) {
            g2d.drawImage(playerImg, playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillOval(playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize);
        }
    }

    private void updateAnimations() {
        boolean animating = false;
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                if (popScale[y][x] > 1.0f) {
                    popScale[y][x] -= 0.02f; // Adjust this to change animation speed
                    animating = true;
                } else {
                    popScale[y][x] = 1.0f;
                }
            }
        }
        // If anything is still "popping", keep the loop going
        if (animating) {
            Timer t = new Timer(16, e -> repaint());
            t.setRepeats(false);
            t.start();
        }
    }

    // Helper methods (Direction, Color, Cooldown, Reset) remain the same...
    private String getDirectionSuffix() {
        if (dirX == 1) return "right";
        if (dirX == -1) return "left";
        if (dirY == 1) return "down";
        if (dirY == -1) return "up";
        return "down";
    }

    private Color getColorForPlayer(int id) {
        return switch (id) {
            case 1 -> new Color(255, 182, 193);
            case 2 -> new Color(152, 251, 152);
            case 3 -> new Color(230, 230, 250);
            case 4 -> new Color(135, 206, 235);
            default -> Color.WHITE;
        };
    }

    public long getCooldownRemaining() {
        long elapsed = System.currentTimeMillis() - lastBurstTime;
        return Math.max(0, (BURST_COOLDOWN - elapsed) / 1000);
    }

    public void reset() {
        this.playerX = 0;
        this.playerY = 0;
        this.dirX = 0;
        this.dirY = 1;
        this.lastBurstTime = 0;
        gameState.setTile(0, 0, playerId);
        repaint();
    }
}