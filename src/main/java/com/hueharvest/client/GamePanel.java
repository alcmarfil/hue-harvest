package com.hueharvest.client;

import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final int TILE_SIZE = 28; 
    private static final double PLAYER_VISUAL_SCALE = 1.4; // player is 40% larger than a tile
    private final GameState gameState;
    
    // movement and direction
    private int playerX = 0; // column the player is
    private int playerY = 0; // row the player is 
    private int dirX = 0; // direction the player is facing
    private int dirY = 1; // start facing down
    private final int playerId = 1; // player id

    // cooldown logic (5 seconds = 5000ms)
    private long lastBurstTime = 0;
    private static final long BURST_COOLDOWN = 5000;

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
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

        // initialize starting position color
        gameState.setTile(playerX, playerY, playerId);
    }

    private void fireInkBurst() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBurstTime < BURST_COOLDOWN) {
            return; // Still on cooldown
        }

        // Projectile lands 3 tiles in front of player
        int targetX = playerX + (dirX * 3);
        int targetY = playerY + (dirY * 3);

        // Paint 3x3 area
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                gameState.setTile(targetX + dx, targetY + dy, playerId);
            }
        }

        lastBurstTime = currentTime;
        repaint();
    }

    // movement and direction
    private void movePlayer(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> { dirX = 0; dirY = -1; }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> { dirX = 0; dirY = 1; }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> { dirX = -1; dirY = 0; }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> { dirX = 1; dirY = 0; }
        }

        playerX += dirX;
        playerY += dirY;

        // bounds check
        if (playerX < 0) playerX = 0;
        if (playerX >= GameState.GRID_SIZE) playerX = GameState.GRID_SIZE - 1;
        if (playerY < 0) playerY = 0;
        if (playerY >= GameState.GRID_SIZE) playerY = GameState.GRID_SIZE - 1;

        // claim tile (set it according to the players color)
        gameState.setTile(playerX, playerY, playerId);
        repaint(); // repaint function
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // draw grid and tiles
        for (int y = 0; y < GameState.GRID_SIZE; y++) {
            for (int x = 0; x < GameState.GRID_SIZE; x++) {
                int tileType = gameState.getTile(x, y);
                
                // try to load tile image (e.g., "tile1.png")
                BufferedImage tileImg = AssetManager.getImage("tile" + tileType + ".png");
                
                if (tileImg != null) {
                    g.drawImage(tileImg, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                } else {
                    // fallback to Color
                    g.setColor(getColorForPlayer(tileType));
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    
                    // subtle Grid lines
                    g2d.setColor(new Color(240, 240, 240));
                    g2d.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        // draw player
        String directionSuffix = getDirectionSuffix();
        BufferedImage playerImg = AssetManager.getImage("player" + playerId + "_" + directionSuffix + ".png");
        
        // final player drawing size (visual scale)
        int drawSize = (int) (TILE_SIZE * PLAYER_VISUAL_SCALE);
        int offset = (drawSize - TILE_SIZE) / 2; // offset to center the larger sprite

        if (playerImg != null) {
            g.drawImage(playerImg, playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize, null);
        } else {
            // default "Ghost" Player rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.BLACK);
            // draw slightly larger than tile
            g2d.fillOval(playerX * TILE_SIZE - offset, playerY * TILE_SIZE - offset, drawSize, drawSize);
            
            // draw "direction eye"
            g2d.setColor(Color.WHITE);
            int eyeSize = 6;
            int ex = (playerX * TILE_SIZE + TILE_SIZE / 2) + (dirX * 8) - (eyeSize / 2);
            int ey = (playerY * TILE_SIZE + TILE_SIZE / 2) + (dirY * 8) - (eyeSize / 2);
            g2d.fillOval(ex, ey, eyeSize, eyeSize);
        }
    }

    // for player sprite direction
    private String getDirectionSuffix() {
        if (dirX == 1) return "right";
        if (dirX == -1) return "left";
        if (dirY == 1) return "down";
        if (dirY == -1) return "up";
        return "down";
    }

    // fallback if textured tile is not rendering
    private Color getColorForPlayer(int id) {
        return switch (id) {
            case 1 -> new Color(255, 182, 193); // pastel pink
            case 2 -> new Color(152, 251, 152); // mint green
            case 3 -> new Color(230, 230, 250); // lavender
            case 4 -> new Color(135, 206, 235); // sky blue
            default -> Color.WHITE; // neutral
        };
    }
}
