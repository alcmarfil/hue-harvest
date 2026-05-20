package com.hueharvest.server;

import com.hueharvest.shared.GameState;
import com.hueharvest.shared.NetworkPacket;

import java.awt.Point;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 12345;
    private final GameState gameState = new GameState();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private int nextPlayerId = 1;

    public void start() {
        System.out.println("Server started on port " + PORT);
        
        // Game Logic Loop (Server-side tick)
        final int broadcastInterval = 16; // ms (~60 FPS)
        final int tickInterval = 1000;    // ms
        final int ticksPerSecond = tickInterval / broadcastInterval;
        final int[] counter = {0};

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            gameState.setNumPlayers(clients.size());
            long now = System.currentTimeMillis();
            for (int i = 1; i <= 4; i++) {
                long last = lastBurstTime.getOrDefault(i, 0L);
                gameState.setBurstReady(i, (now - last) >= BURST_COOLDOWN);
            }
            if (gameState.getStatus() == GameState.Status.PLAYING) {
                counter[0]++;
                if (counter[0] >= ticksPerSecond) {
                    gameState.tick(); // Only decrement time once per second
                    counter[0] = 0;
                }
                // checkGoals() removed - match only ends on time
            }
            broadcast(new NetworkPacket(NetworkPacket.Type.UPDATE, 0, gameState));
        }, 0, broadcastInterval, TimeUnit.MILLISECONDS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                if (clients.size() < 4) {
                    int assignedId = -1;
                    for (int id = 1; id <= 4; id++) {
                        boolean idInUse = false;
                        for (ClientHandler client : clients) {
                            if (client.playerId == id) {
                                idInUse = true;
                                break;
                            }
                        }
                        if (!idInUse) {
                            assignedId = id;
                            break;
                        }
                    }
                    if (assignedId != -1) {
                        ClientHandler handler = new ClientHandler(socket, assignedId);
                        clients.add(handler);
                        new Thread(handler).start();
                    } else {
                        rejectConnection(socket, "Server full (no free player slots).");
                    }
                } else {
                    rejectConnection(socket, "Server full (maximum 4 players).");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rejectConnection(Socket socket, String reason) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeUnshared(new NetworkPacket(NetworkPacket.Type.WELCOME, -1, reason));
            out.flush();
        } catch (IOException e) {
            // Ignore disconnect
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void broadcast(NetworkPacket packet) {
        for (ClientHandler client : clients) {
            client.send(packet);
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                this.socket.setTcpNoDelay(true); // Disable Nagle's algorithm on the server side
            } catch (java.net.SocketException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Send welcome packet
                send(new NetworkPacket(NetworkPacket.Type.WELCOME, playerId, null));

                while (true) {
                    NetworkPacket packet = (NetworkPacket) in.readObject();
                    handlePacket(packet);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Player " + playerId + " disconnected.");
            } finally {
                clients.remove(this);
                playerPositions.remove(playerId);
                playerDirections.remove(playerId);
                lastBurstTime.remove(playerId);
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        private void handlePacket(NetworkPacket packet) {
            switch (packet.type) {
                case START -> {
                    if (playerId == 1) { // Only host can start
                        gameState.setStatus(GameState.Status.PLAYING);
                        initAllPlayers();
                    }
                }
                case MOVE -> {
                    if (gameState.getStatus() == GameState.Status.PLAYING) {
                        int[] dir = (int[]) packet.data;
                        updatePlayerPos(playerId, dir[0], dir[1]);
                    }
                }
                case BURST -> {
                    if (gameState.getStatus() == GameState.Status.PLAYING) {
                        triggerBurst(playerId);
                    }
                }
                case CHAT -> {
                    packet.playerId = this.playerId;
                    broadcast(packet);
                }
                default -> {} 
            }
        }

        public synchronized void send(NetworkPacket packet) {
            if (out == null) return;
            try {
                out.writeUnshared(packet);
                out.flush();
                out.reset(); // Clear object cache to send fresh state
            } catch (IOException e) {
                // Handle silent disconnect
            }
        }
    }

    // Server-side authority for player positions and cooldowns
    private final Map<Integer, Point> playerPositions = new ConcurrentHashMap<>();
    private final Map<Integer, Point> playerDirections = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastBurstTime = new ConcurrentHashMap<>();
    private static final long BURST_COOLDOWN = 5000;

    private void updatePlayerPos(int pId, int dx, int dy) {
        Point pos = playerPositions.getOrDefault(pId, new Point(0, 0)); // Initial pos based on ID in real impl
        Point dir = playerDirections.getOrDefault(pId, new Point(0, 1));
        
        // Simple initialization if first move
        if (!playerPositions.containsKey(pId)) {
            // Match starting positions from GamePanel
            int gs = GameState.GRID_SIZE - 1;
            if (pId == 1) pos = new Point(0, 0);
            else if (pId == 2) pos = new Point(gs, 0);
            else if (pId == 3) pos = new Point(0, gs);
            else if (pId == 4) pos = new Point(gs, gs);
        }

        if (dx != 0 || dy != 0) {
            dir.x = dx;
            dir.y = dy;
        }
        
        pos.x = Math.max(0, Math.min(GameState.GRID_SIZE - 1, pos.x + dx));
        pos.y = Math.max(0, Math.min(GameState.GRID_SIZE - 1, pos.y + dy));
        
        playerPositions.put(pId, pos);
        playerDirections.put(pId, dir);
        gameState.setPlayerPos(pId, pos.x, pos.y, dir.x, dir.y);
        gameState.setTile(pos.x, pos.y, pId);
    }

    private void triggerBurst(int pId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastBurstTime.getOrDefault(pId, 0L);
        if (currentTime - lastTime < BURST_COOLDOWN) return;

        Point pos = playerPositions.get(pId);
        Point dir = playerDirections.get(pId);
        if (pos == null || dir == null) return;

        int targetX = pos.x + (dir.x * 3);
        int targetY = pos.y + (dir.y * 3);

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                gameState.setTile(targetX + dx, targetY + dy, pId);
            }
        }
        lastBurstTime.put(pId, currentTime);
    }

    private void checkGoals() {
        // Disabled as per user request - match only ends on time
    }

    private void initAllPlayers() {
        for (ClientHandler client : clients) {
            updatePlayerPos(client.playerId, 0, 0); // Triggers init in updatePlayerPos
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
