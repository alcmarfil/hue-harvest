package com.hueharvest.shared;

import java.io.Serializable;

public class GameState implements Serializable {
    public static final int GRID_SIZE = 20;
    public static final int GOAL_TILES = 200;
    public static final int INITIAL_TIME = 120;
    
    public enum Status { LOBBY, PLAYING, FINISHED }
    private Status status = Status.LOBBY;
    private int winnerId = -1;
    private int numPlayers = 0;
    
    // 0 = Neutral / Tile not yet taken , 1-4 = Player Colors
    private final int[][] grid;
    private int timeLeft; // in seconds
    
    // Player data for online synchronization
    private final int[] playerX = new int[5]; // Index 1-4
    private final int[] playerY = new int[5];
    private final int[] dirX = new int[5];
    private final int[] dirY = new int[5];
    private final boolean[] burstReady = new boolean[5];
    
    public GameState() {
        this.grid = new int[GRID_SIZE][GRID_SIZE];
        this.timeLeft = INITIAL_TIME;
        for (int i = 0; i < 5; i++) burstReady[i] = true;
    }

    public int[][] getGrid() {
        return grid;
    }

    public void setTile(int x, int y, int playerId) {
        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
            grid[y][x] = playerId;
        }
    }

    public int getTile(int x, int y) {
        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
            return grid[y][x];
        }
        return -1;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public void tick() {
        if (status == Status.PLAYING && timeLeft > 0) timeLeft--;
        if (timeLeft <= 0 && status == Status.PLAYING) {
            status = Status.FINISHED;
        }
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
    public int getNumPlayers() { return numPlayers; }
    public void setNumPlayers(int numPlayers) { this.numPlayers = numPlayers; }

    public void reset() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                grid[y][x] = 0;
            }
        }
        this.timeLeft = INITIAL_TIME; // change kapag final na
        for (int i = 0; i < 5; i++) {
            playerX[i] = playerY[i] = dirX[i] = dirY[i] = 0;
        }
    }

    public void setPlayerPos(int id, int x, int y, int dx, int dy) {
        if (id >= 1 && id <= 4) {
            playerX[id] = x;
            playerY[id] = y;
            dirX[id] = dx;
            dirY[id] = dy;
        }
    }

    public int getPlayerX(int id) { return playerX[id]; }
    public int getPlayerY(int id) { return playerY[id]; }
    public int getDirX(int id) { return dirX[id]; }
    public int getDirY(int id) { return dirY[id]; }
    public boolean isBurstReady(int id) { return burstReady[id]; }
    public void setBurstReady(int id, boolean ready) { burstReady[id] = ready; }

    public int getTileCount(int playerId) {
        int count = 0;
        for (int[] row : grid) {
            for (int tile : row) {
                if (tile == playerId) count++;
            }
        }
        return count;
    }
}
