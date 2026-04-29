package com.hueharvest.shared;

import java.io.Serializable;

public class GameState implements Serializable {
    public static final int GRID_SIZE = 30;
    public static final int GOAL_TILES = 30;
    public static final int INITIAL_TIME = 90;
    
    // 0 = Neutral / Tile not yet taken , 1-4 = Player Colors
    private final int[][] grid;
    private int timeLeft; // in seconds
    
    public GameState() {
        this.grid = new int[GRID_SIZE][GRID_SIZE];
        this.timeLeft = INITIAL_TIME;
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
        if (timeLeft > 0) timeLeft--;
    }

    public void reset() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                grid[y][x] = 0;
            }
        }
        this.timeLeft = INITIAL_TIME; // change kapag final na
    }

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
